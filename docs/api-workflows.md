# API Workflows

This document shows how to test the current platform on Friday, July 17, 2026.

## Overview

The backend currently exposes connected workflows for:

- fraud assessment
- fraud simulation
- threshold comparison simulation
- versioned scoring profile management
- challenge outcome completion and feedback analytics
- reviewer productivity, fraud-case turnaround analytics, reviewer drill-down, workload balancing, and case-priority routing views
- fraud operations summary
- persisted replay batches
- payment lifecycle tracking
- analyst-driven fraud case handling
- persisted fraud-case queue filtering and supervisor CSV export
- durable outbound event delivery tracking
- operator replay of failed outbound events
- outbound-event filtering for incident triage
- outbound retry and aging analytics
- outbound incident note capture
- failed-event CSV export and manual dispatch sweep
- persisted operator authentication and role-backed access control

The important point is that these are now connected. A fraud assessment can create a payment record, a fraud case, and later a final analyst decision that changes the payment outcome.

## 0. Simulate A Fraud Decision Without Persistence

Use this when you want to validate the current rules without creating any records:

```bash
curl -X POST http://localhost:8080/api/v1/fraud-assessments/simulations ^
  -u ingest.client:local-ingest-2026 ^
  -H "Content-Type: application/json" ^
  -d "{\
    \"paymentId\":\"PAY-SIM-3001\",\
    \"customerId\":\"CUST-SIM-3001\",\
    \"amount\":9000.00,\
    \"currency\":\"ZAR\",\
    \"merchantCategory\":\"ELECTRONICS\",\
    \"paymentChannel\":\"MOBILE_APP\",\
    \"beneficiaryAgeHours\":3,\
    \"transactionCountLastFiveMinutes\":3,\
    \"spendLastHour\":11000.00,\
    \"customerAverageTicket\":1800.00,\
    \"newDevice\":true,\
    \"highRiskCountry\":false,\
    \"impossibleTravel\":false,\
    \"recentPasswordReset\":false\
  }"
```

Simulation-specific expectations:

- no `assessmentId` is created because nothing is persisted
- no payment record is created
- no fraud case is created
- the response still shows the projected decision and whether a review case would be needed

## 0.1 Compare Baseline And Override Thresholds

Use this when you want to test how a rule-threshold adjustment would change the outcome for the same payment scenario:

```bash
curl -X POST http://localhost:8080/api/v1/fraud-assessments/simulations/compare ^
  -u analyst.one:local-analyst-2026 ^
  -H "Content-Type: application/json" ^
  -d "{\
    \"scenario\":{\
      \"paymentId\":\"PAY-COMPARE-3001\",\
      \"customerId\":\"CUST-COMPARE-3001\",\
      \"amount\":9000.00,\
      \"currency\":\"ZAR\",\
      \"merchantCategory\":\"ELECTRONICS\",\
      \"paymentChannel\":\"MOBILE_APP\",\
      \"beneficiaryAgeHours\":3,\
      \"transactionCountLastFiveMinutes\":3,\
      \"spendLastHour\":11000.00,\
      \"customerAverageTicket\":1800.00,\
      \"newDevice\":true,\
      \"highRiskCountry\":false,\
      \"impossibleTravel\":false,\
      \"recentPasswordReset\":false\
    },\
    \"overrides\":{\
      \"challengeThreshold\":35,\
      \"holdThreshold\":55,\
      \"declineThreshold\":80\
    }\
  }"
```

Comparison-specific expectations:

- the response contains both `baselineOutcome` and `overrideOutcome`
- each outcome shows the thresholds that were used
- nothing is persisted even when the override outcome would create a review case

## 0.15 Manage Versioned Scoring Profiles

Use these routes when you want threshold changes to be reviewable, comparable, and promotable instead of relying only on one-off overrides.

Supervisor-grade promotions, exports, and replay controls now require a short-lived step-up verification flow before the protected command will succeed.

Generate a one-time token:

```bash
curl -X POST http://localhost:8080/api/v1/security/step-up/token ^
  -u senior.analyst:local-senior-2026
```

Then verify it as the same operator:

```bash
curl -u senior.analyst:local-senior-2026 ^
  "http://localhost:8080/api/v1/security/step-up/verify?token={token}"
```

Step-up expectations:

- the token is stored in the database and expires after a short validity window
- the verification must be completed by the same authenticated operator who generated the token
- the elevated verification is session-scoped and currently gates supervisor-grade profile activation, case export, final case resolution, and outbound replay controls
- the default local backend profile sends the message through Mailpit on `localhost:1025`, and the inbox can be inspected at `http://localhost:8025`
- test-oriented `DEVELOPMENT_LINK` mode can still expose the verification URL directly when deterministic automation is more useful than SMTP delivery
- requesting a resend invalidates any prior open token for that operator before delivering the replacement token
- consumed, revoked, expired, failed, or previously replaced tokens cannot be replayed successfully
- every issuance and delivery attempt is written to a delivery-audit table so operators can review the lifecycle later

Resend a token when you need a fresh delivery:

```bash
curl -X POST http://localhost:8080/api/v1/security/step-up/token/resend ^
  -u senior.analyst:local-senior-2026
```

Read the step-up delivery audit trail:

```bash
curl -u senior.analyst:local-senior-2026 ^
  "http://localhost:8080/api/v1/security/step-up/deliveries?limit=10"
```

Platform administrators can also filter the audit trail:

```bash
curl -u platform.admin:local-admin-2026 ^
  "http://localhost:8080/api/v1/security/step-up/deliveries?operator=senior.analyst&status=SENT&limit=20"
```

Revoke any still-open step-up tokens if the session should be reset:

```bash
curl -X POST http://localhost:8080/api/v1/security/step-up/revoke ^
  -u senior.analyst:local-senior-2026
```

Create a draft profile:

```bash
curl -X POST http://localhost:8080/api/v1/fraud-assessments/scoring-profiles ^
  -u analyst.one:local-analyst-2026 ^
  -H "Content-Type: application/json" ^
  -d "{\
    \"profileName\":\"July rollout candidate\",\
    \"challengeThreshold\":42,\
    \"holdThreshold\":63,\
    \"declineThreshold\":84,\
    \"changeSummary\":\"Lower early friction while keeping the decline boundary conservative.\"\
  }"
```

Read the active profile:

```bash
curl -u analyst.one:local-analyst-2026 http://localhost:8080/api/v1/fraud-assessments/scoring-profiles/active
```

Compare a saved profile against the live one:

```bash
curl -X POST http://localhost:8080/api/v1/fraud-assessments/simulations/compare-saved-profile/{profileId} ^
  -u analyst.one:local-analyst-2026 ^
  -H "Content-Type: application/json" ^
  -d "{\
    \"paymentId\":\"PAY-COMPARE-4001\",\
    \"customerId\":\"CUST-COMPARE-4001\",\
    \"amount\":9000.00,\
    \"currency\":\"ZAR\",\
    \"merchantCategory\":\"ELECTRONICS\",\
    \"paymentChannel\":\"MOBILE_APP\",\
    \"beneficiaryAgeHours\":3,\
    \"transactionCountLastFiveMinutes\":3,\
    \"spendLastHour\":11000.00,\
    \"customerAverageTicket\":1800.00,\
    \"newDevice\":true,\
    \"highRiskCountry\":false,\
    \"impossibleTravel\":false,\
    \"recentPasswordReset\":false\
  }"
```

Activate an approved profile:

```bash
curl -X POST http://localhost:8080/api/v1/fraud-assessments/scoring-profiles/{profileId}/activate ^
  -u senior.analyst:local-senior-2026 ^
  -H "Content-Type: application/json" ^
  -d "{}"
```

Profile-management expectations:

- draft profiles are stored with version numbers and remain inactive until explicitly promoted
- the live scoring path uses only the active profile
- if no stored profile has been activated yet, the API surfaces the system-default property-backed thresholds as the active profile
- supervisors must complete the step-up verification flow before activating a profile

## 0.2 Read The Fraud Operations Summary

Use this when you want the current aggregate picture without opening each payment or case one by one:

```bash
curl -u analyst.one:local-analyst-2026 http://localhost:8080/api/v1/fraud-operations/summary
```

Summary-specific expectations:

- the response includes total assessments, payments, and cases
- `reviewBacklogCount` reflects open plus escalated cases
- `challengeOutcomeSummary` shows how many challenged payments are pending, passed, failed, or abandoned
- `reviewerAnalytics` shows turnaround, backlog age, backlog SLA breach counts, per-reviewer drill-down, workload balancing recommendations, case-priority routing hints, and supervisor intervention signals
- `outboundDeliverySummary` shows pending, delivered, and failed outbound fraud events
- grouped metric arrays summarize decisions, payment states, case states, and resolution outcomes

## 0.22 Complete A Challenge Outcome

Use this when a challenged payment finishes the customer verification flow and you want the result to be auditable:

```bash
curl -X POST http://localhost:8080/api/v1/payments/PAY-CHALLENGE-1001/challenge-outcome ^
  -u analyst.one:local-analyst-2026 ^
  -H "Content-Type: application/json" ^
  -d "{\
    \"outcome\":\"ABANDONED\",\
    \"note\":\"Customer did not complete the OTP flow within the allowed window.\"\
  }"
```

Challenge-outcome expectations:

- only payments currently in `CHALLENGED` status can use this route
- `PASSED` moves the payment to `APPROVED`
- `FAILED` and `ABANDONED` move the payment to `DECLINED`
- the payment record keeps the outcome, authenticated operator, note, and completion timestamp for later tuning review

## 0.25 Inspect Failed Outbound Events

Use this when you want to understand whether downstream notification or orchestration events are backing up or failing:

```bash
curl -u analyst.one:local-analyst-2026 ^
  "http://localhost:8080/api/v1/fraud-operations/outbound-events?status=FAILED&limit=25"
```

You can also narrow the view further when triaging a specific incident:

```bash
curl -u analyst.one:local-analyst-2026 ^
  "http://localhost:8080/api/v1/fraud-operations/outbound-events?status=FAILED&topicName=fraud-notifications&eventType=CASE_ESCALATED&messageKey=PAY-REVIEW&onlyWithNotes=true&limit=25"
```

Outbound-operations expectations:

- each item identifies the event id, event type, Kafka topic, and message key
- `attemptCount` shows how many publish attempts already happened
- `lastError` captures the latest broker or serialization failure reason
- `nextAttemptAt` shows when a pending retry is scheduled
- `operatorNote`, `notedBy`, and `notedAt` show any incident annotation already attached to the event, with `notedBy` derived from the authenticated operator

## 0.252 Read Outbound Delivery Analytics

Use this when you want a compact operational view of retry pressure, daily delivery shape, and how long failed incidents have been sitting open:

```bash
curl -u analyst.one:local-analyst-2026 ^
  "http://localhost:8080/api/v1/fraud-operations/outbound-events/analytics?days=7"
```

Analytics expectations:

- `retryMetrics` shows total events in the time window, how many required retries, retry rate, delivered-after-retry count, failed-after-retry count, and average attempts
- `dailyTrends` returns one row per day with pending, delivered, and failed counts
- `incidentAging` groups currently failed events into age buckets and splits each bucket into noted versus unnoted incidents

## 0.255 Add An Outbound Incident Note

Use this when an analyst or supervisor wants to preserve incident context directly on the failed outbound event record:

```bash
curl -X POST -u analyst.one:local-analyst-2026 ^
  http://localhost:8080/api/v1/fraud-operations/outbound-events/{eventId}/incident-note ^
  -H "Content-Type: application/json" ^
  -d "{\
    \"note\":\"Kafka broker recovered, but this event still needs replay after downstream schema validation.\"\
  }"
```

Incident-note expectations:

- the response returns the updated outbound event record
- `operatorNote`, `notedBy`, and `notedAt` are populated
- the note stays attached to the event for CSV export and later operator review

## 0.26 Requeue One Failed Outbound Event

Use this when a supervisor wants to requeue a specific failed event after the downstream dependency recovers:

```bash
curl -X POST -u senior.analyst:local-senior-2026 ^
  http://localhost:8080/api/v1/fraud-operations/outbound-events/{eventId}/retry
```

Single-retry expectations:

- the event status returns as `PENDING`
- `nextAttemptAt` is moved to the current retry request time
- delivered events are rejected because they do not need replay
- supervisors must complete the step-up verification flow before retrying the event

## 0.27 Export Failed Outbound Events

Use this when you want a spreadsheet-friendly extract of failed outbound records for an incident review:

```bash
curl -u analyst.one:local-analyst-2026 ^
  "http://localhost:8080/api/v1/fraud-operations/outbound-events/failed-export?limit=25"
```

Export expectations:

- the response content type is `text/csv`
- the export includes event identity, topic, attempt counts, timestamps, last error details, and any saved incident note metadata
- analysts can read the export without needing supervisor permissions

## 0.28 Requeue Failed Outbound Events In Batch

Use this when a downstream outage is resolved and you want to put a batch of failed records back into the dispatcher queue:

```bash
curl -X POST -u senior.analyst:local-senior-2026 ^
  "http://localhost:8080/api/v1/fraud-operations/outbound-events/retry-failed?limit=25"
```

Batch-retry expectations:

- the response returns the requested batch size and how many failed events were requeued
- only failed events are moved back to `PENDING`
- supervisors must complete the step-up verification flow before batch replay

## 0.29 Trigger An Immediate Dispatch Sweep

Use this when you want the platform to try pending outbound events immediately rather than waiting for the next scheduled dispatcher cycle:

```bash
curl -X POST -u senior.analyst:local-senior-2026 ^
  http://localhost:8080/api/v1/fraud-operations/outbound-events/dispatch-now
```

Dispatch-sweep expectations:

- the response returns how many pending events were processed in that sweep
- this is useful right after replaying failed events or after a broker outage is cleared
- supervisors must complete the step-up verification flow before dispatching immediately

## 0.3 Create A Historical Replay Batch

Use this when you want to rerun a set of scenarios under the active thresholds or an override profile and keep the result set:

```bash
curl -X POST http://localhost:8080/api/v1/fraud-replays ^
  -u analyst.one:local-analyst-2026 ^
  -H "Content-Type: application/json" ^
  -d "{\
    \"batchName\":\"July 17 replay batch\",\
    \"overrides\":{\
      \"challengeThreshold\":40,\
      \"holdThreshold\":60,\
      \"declineThreshold\":80\
    },\
    \"scenarios\":[\
      {\
        \"paymentId\":\"PAY-REPLAY-3001\",\
        \"customerId\":\"CUST-REPLAY-3001\",\
        \"amount\":9000.00,\
        \"currency\":\"ZAR\",\
        \"merchantCategory\":\"ELECTRONICS\",\
        \"paymentChannel\":\"MOBILE_APP\",\
        \"beneficiaryAgeHours\":3,\
        \"transactionCountLastFiveMinutes\":3,\
        \"spendLastHour\":11000.00,\
        \"customerAverageTicket\":1800.00,\
        \"newDevice\":true,\
        \"highRiskCountry\":false,\
        \"impossibleTravel\":false,\
        \"recentPasswordReset\":false\
      }\
    ]\
  }"
```

Replay-specific expectations:

- the response returns a persisted `batchId`
- every scenario result is itemized under `items`
- grouped decision and projected-payment counts summarize the replay outcome

## 1. Submit A High-Risk Payment

Use this request to create a likely reviewable payment:

```bash
curl -X POST http://localhost:8080/api/v1/fraud-assessments ^
  -u ingest.client:local-ingest-2026 ^
  -H "Content-Type: application/json" ^
  -d "{\
    \"paymentId\":\"PAY-REVIEW-1001\",\
    \"customerId\":\"CUST-42\",\
    \"amount\":12500.00,\
    \"currency\":\"ZAR\",\
    \"merchantCategory\":\"ELECTRONICS\",\
    \"paymentChannel\":\"MOBILE_APP\",\
    \"beneficiaryAgeHours\":1,\
    \"transactionCountLastFiveMinutes\":5,\
    \"spendLastHour\":24000.00,\
    \"customerAverageTicket\":1800.00,\
    \"newDevice\":true,\
    \"highRiskCountry\":false,\
    \"impossibleTravel\":true,\
    \"recentPasswordReset\":true\
  }"
```

Look for these fields in the response:

- `assessmentId`
- `reviewCaseId`
- `decision`
- `paymentStatus`
- `triggeredFactors`

Typical high-risk outcomes are:

- `HOLD` with payment status `HELD`
- `DECLINE` with payment status `DECLINED`

## 2. Inspect The Payment Record

```bash
curl -u analyst.one:local-analyst-2026 http://localhost:8080/api/v1/payments/PAY-REVIEW-1001
```

The payment response now shows:

- the latest decision
- the current payment status
- the latest assessment id
- a transition history

Typical statuses:

- `APPROVED`
- `CHALLENGED`
- `HELD`
- `DECLINED`

## 3. Inspect The Fraud Case

If the assessment returned a `reviewCaseId`, inspect it:

```bash
curl -u analyst.one:local-analyst-2026 http://localhost:8080/api/v1/fraud-cases/{caseId}
```

The case response includes:

- review status
- assignee
- resolution summary
- resolution outcome
- timeline entries

## 3.1 Filter The Fraud Case Queue

Use this when an analyst needs to triage only a focused part of the persisted backlog:

```bash
curl -u analyst.one:local-analyst-2026 ^
  "http://localhost:8080/api/v1/fraud-cases?status=OPEN&minRiskScore=70&unassignedOnly=true&paymentId=PAY-REVIEW"
```

Queue-filter expectations:

- the response stays ordered by newest `createdAt`
- filters are applied in the database, not after loading the full queue
- `breachedOnly=true` limits the result to open or escalated cases older than the 24-hour review SLA
- `paymentId` and `customerId` support partial matching for operational lookup

## 3.2 Export The Fraud Case Queue

Use this when a supervisor needs a spreadsheet-friendly queue extract for escalations or shift handoff:

```bash
curl -u senior.analyst:local-senior-2026 ^
  "http://localhost:8080/api/v1/fraud-cases/export?status=ESCALATED&breachedOnly=true"
```

Queue-export expectations:

- only `senior.analyst` and `platform.admin` can read the export
- the response content type is `text/csv`
- the export reuses the same persisted filters as the JSON queue endpoint
- each row includes case identity, payment/customer references, assignee, timestamps, SLA breach status, and resolution fields
- supervisors must complete the step-up verification flow before exporting the queue

## 4. Assign The Case

```bash
curl -X POST http://localhost:8080/api/v1/fraud-cases/{caseId}/assign ^
  -u analyst.one:local-analyst-2026 ^
  -H "Content-Type: application/json" ^
  -d "{\
    \"assignee\":\"analyst.one\",\
    \"note\":\"Taking ownership for first-line review.\"\
  }"
```

## 5. Escalate The Case

```bash
curl -X POST http://localhost:8080/api/v1/fraud-cases/{caseId}/escalate ^
  -u senior.analyst:local-senior-2026 ^
  -H "Content-Type: application/json" ^
  -d "{\
    \"reason\":\"Needs a senior investigator because the beneficiary risk is still unclear.\"\
  }"
```

## 6. Add An Analyst Note

```bash
curl -X POST http://localhost:8080/api/v1/fraud-cases/{caseId}/notes ^
  -u analyst.one:local-analyst-2026 ^
  -H "Content-Type: application/json" ^
  -d "{\
    \"note\":\"Customer verified device ownership during outbound callback.\"\
  }"
```

## 7. Release A Held Payment

Use this when the case should end in customer approval:

```bash
curl -X POST http://localhost:8080/api/v1/fraud-cases/{caseId}/release ^
  -u senior.analyst:local-senior-2026 ^
  -H "Content-Type: application/json" ^
  -d "{\
    \"resolutionSummary\":\"Released after step-up verification and callback confirmation.\"\
  }"
```

After this call:

- the case status should become `RESOLVED`
- the case resolution outcome should be `RELEASE_PAYMENT`
- the payment status should become `APPROVED`
- supervisors must complete the step-up verification flow before releasing the payment

## 8. Confirm A Decline

Use this when the payment should stay rejected:

```bash
curl -X POST http://localhost:8080/api/v1/fraud-cases/{caseId}/confirm-decline ^
  -u senior.analyst:local-senior-2026 ^
  -H "Content-Type: application/json" ^
  -d "{\
    \"resolutionSummary\":\"Decline confirmed after beneficiary and account-risk review.\"\
  }"
```

After this call:

- the case status should become `RESOLVED`
- the case resolution outcome should be `CONFIRM_DECLINE`
- the payment status should become `DECLINED`
- supervisors must complete the step-up verification flow before confirming the decline

## 9. Generic Resolution Endpoint

The lower-level generic endpoint still exists:

```bash
POST /api/v1/fraud-cases/{caseId}/resolve
```

It accepts:

- `resolutionSummary`
- `outcome`

The authenticated operator is derived from the signed-in principal. This route is useful for internal integrations, but the dedicated `release` and `confirm-decline` routes are clearer for analyst tooling.
This route also requires step-up verification for supervisor-grade use.

## Business Rules To Know

- Resolved cases cannot be assigned or escalated again.
- Notes can still be added after resolution.
- A case created from an immediate `DECLINE` decision cannot be released under the current rules.
- Payment history is auditable through `/api/v1/payments/{paymentId}`.
- Supervisor-grade protected commands require a successful step-up verification in the same authenticated session before execution.
- Resending or revoking a token invalidates any earlier open token for that operator.

## Suggested Demo Script

1. Submit a high-risk payment.
2. Open the resulting payment record.
3. Open the resulting fraud case.
4. Assign the case.
5. Add a note.
6. Either release the payment or confirm decline.
7. Re-read both the payment and case endpoints to confirm the final state and history.
