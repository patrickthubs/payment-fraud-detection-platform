# Payment Fraud Detection Platform

`payment-fraud-detection-platform` is a production-oriented Spring Boot project for exploring how banks and payment platforms make real-time fraud decisions without pretending to reproduce private bank systems exactly.

The project focuses on explainable fraud decisioning:
- ingest a payment risk assessment request
- evaluate fraud signals such as velocity, device novelty, beneficiary risk, location mismatch, and merchant risk
- compute a risk score and recommended action
- return the factors that drove the decision

## Why This Project Exists

Banks do not usually rely on one magic rule for fraud. They combine:
- customer history
- transaction velocity
- device and channel signals
- beneficiary and merchant risk
- location anomalies
- account security events

This project models that style of decisioning with a transparent rules-based engine first. Kafka, Redis, and PostgreSQL are included in the architecture because they are realistic building blocks for event-driven fraud systems, but the initial implementation keeps the logic explainable and testable.

## What Is In Scope Today

- Spring Boot 4 backend service
- explainable fraud scoring endpoint
- immutable decision provenance with complete rule, input, factor, and profile snapshots
- idempotent assessment submission through the `Idempotency-Key` header
- confirmed fraud outcome labels and decision-quality metrics
- non-persistent fraud simulation endpoint
- threshold comparison simulation tooling
- persisted replay batches for historical scenario analysis
- persisted payment lifecycle with status transitions
- persisted challenge completion outcomes for tuning feedback
- downstream payment orchestration events
- notification hooks for challenge, hold, decline, and review outcomes
- durable outbox delivery with retries for outbound fraud events
- operator APIs for outbound event visibility and replay
- dead-letter style CSV export for failed outbound events
- fraud case review queue with analyst actions
- Redis-aware velocity feature abstraction
- Kafka assessment event publishing
- Kafka topic conventions and local Docker stack
- secure step-up verification with delivery audit, resend, revoke, and replay protection
- server-side operator sessions with CSRF protection and no browser password persistence
- architecture documentation grounded in real payment-fraud concepts
- workflow documentation for assessment, payment, and case operations
- unit and integration-style application boot coverage

## What Is Deliberately Not Claimed

- this is not a bank-grade real payment switch
- this is not an AML or sanctions engine
- this does not guarantee real fraud prevention outcomes
- this does not model private card-network or bank-core proprietary controls

## Architecture Overview

Current implementation:
- `backend`: single Spring Boot 4 service for fraud assessment, case management, payment lifecycle, replay batches, scoring profiles, outbound operations, reviewer analytics, and step-up security
- `ui`: Angular operator console for the Fraud Operations Command Center, case review, payments, Risk Rules Studio, simulations, decision quality, and recovery operations

Supporting infrastructure:
- Kafka for payment and decision events
- Redis for low-latency velocity and short-lived features
- PostgreSQL for cases, reviews, and audit metadata

See [docs/architecture.md](C:/Users/ntsatsi.thubakgale/NEW_PROJECTS/payment-fraud-detection-platform/docs/architecture.md) for the detailed design.
See [docs/api-workflows.md](C:/Users/ntsatsi.thubakgale/NEW_PROJECTS/payment-fraud-detection-platform/docs/api-workflows.md) for endpoint-level testing flows.

## Product Surfaces

The UI now behaves like a follow-up fraud operations product rather than a backend demo:

- `Command Center`: the default operator screen for live backlog pressure, priority review cases, payment holds, active policy posture, and quick case actions.
- `Rules Studio`: a supervisor workbench for creating threshold and rule-weight policy drafts, comparing a candidate policy against the live policy, and activating saved versions.
- `Decision Quality`: ground-truth labelling and precision/recall feedback so policy tuning is based on confirmed outcomes rather than guesses.
- `Operations`: outbound event recovery, replay batches, and step-up delivery audit.

## SaaS Foundation

The platform now has an explicit organization boundary for commercial SaaS evolution:

- `fraud_organizations` stores tenant identity, plan code, lifecycle status, and timestamps.
- every persisted operator belongs to exactly one organization through `fraud_operators.organization_id`.
- authenticated browser sessions return the operator's organization metadata so the UI and APIs have a stable tenant context.
- core business records now carry `organization_id`: assessments, review cases, case timeline entries, payments, payment transitions, scoring profiles, outcomes, replay batches, replay items, and outbound events.
- step-up delivery audit and operator security state rows are tenant-owned, so privileged-action verification history and rate-limit state stay scoped to the operator's organization.
- operator-facing repository/service paths scope reads, writes, dashboards, exports, rules, outcomes, replay batches, and outbound incident workflows to the authenticated organization.
- the local demo runs under `Signal Desk Demo Bank`, while production can seed real organizations without carrying reusable demo operators.

This uses a shared-database, tenant-column model. Background dispatcher jobs can still process ready outbound events globally because they are internal workers; operator-visible outbound views remain tenant-scoped. Local demo fallback is limited to explicit local/demo tenant resolution, while service and repository paths use explicit organization context.

## Tech Stack

- Java 26
- Spring Boot 4.x
- Maven
- Spring Web
- Spring Security
- Spring Validation
- Spring Kafka
- Spring Data Redis
- Springdoc OpenAPI / Swagger UI
- PostgreSQL
- Flyway-ready local platform direction
- Docker Compose
- JUnit 5

## Project Structure

```text
payment-fraud-detection-platform/
|- backend/
|  |- Dockerfile
|  |- pom.xml
|  \- src/
|- ui/
|  |- Dockerfile
|  |- nginx.conf
|  |- package.json
|  \- src/
|- docs/
|  |- api-workflows.md
|  |- architecture.md
|  \- smoke-test.md
|- docker-compose.yml
\- README.md
```

## Core Fraud Metrics Modeled

The current decision engine uses signals like:
- amount vs customer baseline
- transaction count in the last 5 minutes
- cumulative spend in the last hour
- new device usage
- impossible travel indicator
- newly added beneficiary
- recent password reset
- risky merchant category
- high-risk geography indicator

The response returns:
- normalized risk score
- recommended action
- resulting payment status
- triggered factors
- analyst-style explanation summary

## Docker Desktop Run

Build and start the complete application stack:

```bash
docker compose up -d --build
```

Docker Desktop will show six containers under the `payment-fraud-detection-platform` Compose project:

- `ui`: Angular production bundle served by Nginx at `http://localhost:4200`
- `backend`: Spring Boot API at `http://localhost:8080`
- `postgres`: PostgreSQL at `localhost:15432`
- `redis`: Redis at `localhost:16379`
- `kafka`: Kafka at `localhost:19092`
- `mailpit`: email viewer at `http://localhost:8025`

The UI proxies `/api` and `/actuator` to the backend inside the Docker network, preserving same-origin sessions and CSRF protection. PostgreSQL and Redis use named volumes so data survives container recreation.

Inspect status and logs from the terminal or select any service in Docker Desktop:

```bash
docker compose ps
docker compose logs -f
docker compose logs -f backend
docker compose logs -f ui
```

Application and infrastructure logs are written to container stdout/stderr with local log rotation. Rebuild one application after source changes with `docker compose up -d --build backend` or `docker compose up -d --build ui`.

Stop the stack without deleting data:

```bash
docker compose down
```

Use `docker compose down -v` only when you intentionally want to delete the PostgreSQL and Redis volumes.

## Local Run

### 1. Start infrastructure

```bash
docker compose up -d postgres redis kafka mailpit
```

Local host ports:
- PostgreSQL: `localhost:15432`
- Redis: `localhost:16379`
- Kafka: `localhost:19092`
- Mailpit SMTP: `localhost:1025`
- Mailpit web UI: `http://localhost:8025`

### 2. Run the backend

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The backend defaults already point to the Redis and Kafka ports above, so you only need environment overrides if your workstation uses different ports.

Local step-up email delivery is now wired to Mailpit by default. That means supervisor and platform-admin verification emails are sent over SMTP to the Mailpit inbox viewer instead of exposing a verification URL directly from the main backend profile.

### 3. Local demo credentials

Use these HTTP Basic accounts for local testing:

- `ingest.client / local-ingest-2026`: submit fraud assessments
- `analyst.one / local-analyst-2026`: read queues, assign cases, add notes
- `senior.analyst / local-senior-2026`: analyst access plus escalate, release, and confirm declines
- `platform.admin / local-admin-2026`: full local access including Swagger/OpenAPI endpoints

These operators are created only by the explicit `local` profile (`fraud.security.demo-users-enabled=true`). Demo users are disabled by default and explicitly disabled again by the `prod` profile. Flyway removes known demo identities before the local initializer runs, so production does not inherit reusable demonstration credentials.

The Angular console exchanges the password once for a server-side session. It does not store passwords in `localStorage` or attach a Basic Auth header to every browser request. HTTP Basic remains available only in local/test configuration for machine clients and command-line examples.

Production machine clients use OAuth2 client-credentials access tokens. The `prod` profile requires `OAUTH2_ISSUER_URI`, validates the `aud` claim against `OAUTH2_AUDIENCE` (default `fraud-api`), maps `OAUTH2_ROLES_CLAIM` (default `roles`) values such as `SCORING_CLIENT` to Spring Security roles, and resolves tenant context from `OAUTH2_ORGANIZATION_ID_CLAIM` (default `organization_id`). Browser sessions and machine bearer tokens coexist without sharing credentials.

### 4. Call the assessment endpoint

```bash
curl -X POST http://localhost:8080/api/v1/fraud-assessments ^
  -u ingest.client:local-ingest-2026 ^
  -H "Idempotency-Key: smoke-PAY-1001" ^
  -H "Content-Type: application/json" ^
  -d "{\
    \"paymentId\":\"PAY-1001\",\
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

Expected behavior:
- low-risk assessments return `ALLOW` and set payment status to `APPROVED`
- medium-risk assessments can return `CHALLENGE` and set payment status to `CHALLENGED`
- higher-risk assessments can return `HOLD` and create a fraud case plus a `HELD` payment
- extreme-risk assessments can return `DECLINE` and create a fraud case plus a `DECLINED` payment

## Smoke Test

If someone clones the project for the first time, this is the fastest reliable local validation path:

1. Clone the repository and open the project root.
2. Start infrastructure with `docker compose up -d`.
3. Start the backend with:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

4. Confirm health:

```bash
curl http://localhost:8080/actuator/health
```

5. Open Swagger:

- `http://localhost:8080/swagger-ui.html`
- sign in with `platform.admin / local-admin-2026`

6. Submit one assessment:

```bash
curl -X POST http://localhost:8080/api/v1/fraud-assessments ^
  -u ingest.client:local-ingest-2026 ^
  -H "Content-Type: application/json" ^
  -d "{\
    \"paymentId\":\"PAY-SMOKE-1001\",\
    \"customerId\":\"CUST-SMOKE-1001\",\
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

7. Inspect the generated queue and payment records:

```bash
curl -u analyst.one:local-analyst-2026 http://localhost:8080/api/v1/fraud-cases
curl -u analyst.one:local-analyst-2026 http://localhost:8080/api/v1/payments/PAY-SMOKE-1001
```

8. Validate supervisor step-up:

- request `POST /api/v1/security/step-up/token`
- open Mailpit at `http://localhost:8025`
- open the delivered email for `senior.analyst@internal.local`
- use the verification link
- retry a protected endpoint such as `/api/v1/fraud-cases/export`

For a longer guided walkthrough, use [docs/api-workflows.md](C:/Users/ntsatsi.thubakgale/NEW_PROJECTS/payment-fraud-detection-platform/docs/api-workflows.md).

## API Areas

- `POST /api/v1/fraud-assessments`: score a payment and persist the resulting payment state
- `POST /api/v1/auth/session`: exchange operator credentials for a CSRF-protected server session
- `GET /api/v1/auth/session`: inspect the current operator session without exposing credentials
- `POST /api/v1/auth/logout`: invalidate the current operator session
- `POST /api/v1/fraud-assessments/simulations`: score a payment scenario without persisting records
- `POST /api/v1/fraud-assessments/simulations/compare`: compare baseline and override thresholds without persisting records
- `POST /api/v1/fraud-assessments/simulations/compare-saved-profile/{profileId}`: compare the live thresholds against a stored candidate profile without persisting records
- `GET /api/v1/fraud-assessments/rules`: inspect the active fraud scoring thresholds
- `GET /api/v1/fraud-assessments/scoring-profiles/active`: inspect the active versioned fraud scoring profile
- `GET /api/v1/fraud-assessments/scoring-profiles`: list draft and active fraud scoring profiles
- `POST /api/v1/fraud-assessments/scoring-profiles`: create a versioned fraud scoring profile without activating it
- `POST /api/v1/fraud-assessments/scoring-profiles/{profileId}/activate`: promote one saved fraud scoring profile to live usage
- `POST /api/v1/fraud-outcomes/assessments/{assessmentId}`: record or correct a supervisor-confirmed fraud outcome
- `GET /api/v1/fraud-outcomes/assessments/{assessmentId}`: inspect the ground-truth outcome for an assessment
- `GET /api/v1/fraud-outcomes/quality-metrics`: inspect precision, recall, false-positive rate, and loss recovery
- `GET /api/v1/fraud-operations/summary`: view aggregate fraud operations metrics
- `GET /api/v1/fraud-operations/outbound-events`: inspect recent outbound delivery records with status, topic, event-type, message-key, and note filters
- `GET /api/v1/fraud-operations/outbound-events/analytics`: view retry health, daily delivery trends, and failed-incident aging
- `GET /api/v1/fraud-operations/outbound-events/failed-export`: export failed outbound events as CSV
- `POST /api/v1/fraud-operations/outbound-events/{eventId}/incident-note`: capture operator incident notes on an outbound event
- `POST /api/v1/fraud-operations/outbound-events/{eventId}/retry`: requeue one failed outbound event
- `POST /api/v1/fraud-operations/outbound-events/retry-failed`: requeue a batch of failed outbound events
- `POST /api/v1/fraud-operations/outbound-events/dispatch-now`: trigger an immediate dispatch sweep
- `POST /api/v1/fraud-replays`: run and persist a replay batch
- `GET /api/v1/fraud-replays`: list replay batches
- `GET /api/v1/fraud-replays/{batchId}`: inspect one replay batch and its scenario results
- `GET /api/v1/payments`: list tracked payments
- `GET /api/v1/payments/{paymentId}`: inspect one payment and its state transitions
- `POST /api/v1/payments/{paymentId}/challenge-outcome`: record whether a challenged payment passed, failed, or was abandoned
- `GET /api/v1/fraud-cases`: list review cases
- `GET /api/v1/fraud-cases/export`: export filtered review cases as CSV for supervisor triage
- `GET /api/v1/fraud-cases/{caseId}`: inspect one case and its timeline
- `POST /api/v1/fraud-cases/{caseId}/assign`: assign a case
- `POST /api/v1/fraud-cases/{caseId}/escalate`: escalate a case
- `POST /api/v1/fraud-cases/{caseId}/notes`: append an analyst note
- `POST /api/v1/fraud-cases/{caseId}/release`: release a held payment through analyst review
- `POST /api/v1/fraud-cases/{caseId}/confirm-decline`: finalize a declined outcome through analyst review
- `POST /api/v1/fraud-cases/{caseId}/resolve`: lower-level generic resolution endpoint kept for flexibility
- `POST /api/v1/security/step-up/token`: issue a short-lived step-up verification token for the authenticated supervisor or platform admin
- `POST /api/v1/security/step-up/token/resend`: invalidate the prior open token and deliver a fresh step-up token
- `GET /api/v1/security/step-up/verify`: verify a token and elevate the current authenticated session
- `POST /api/v1/security/step-up/revoke`: revoke the authenticated operator's outstanding open step-up tokens
- `GET /api/v1/security/step-up/deliveries`: inspect tenant-scoped step-up delivery audit history, with operator filtering for platform administrators

## End-To-End Flow

1. Submit a payment to `POST /api/v1/fraud-assessments`.
2. Inspect the resulting payment using `GET /api/v1/payments/{paymentId}`.
3. If a case was created, fetch it from `GET /api/v1/fraud-cases/{caseId}`.
4. Use analyst commands such as `assign`, `escalate`, `release`, or `confirm-decline`.
5. Recheck the payment and case records to confirm the final disposition and audit trail.

Full request examples live in [docs/api-workflows.md](C:/Users/ntsatsi.thubakgale/NEW_PROJECTS/payment-fraud-detection-platform/docs/api-workflows.md).

## Case Queue Triage

Use persisted queue filters when analysts or supervisors need to narrow the review backlog without exporting everything:

```bash
curl -u analyst.one:local-analyst-2026 ^
  "http://localhost:8080/api/v1/fraud-cases?status=OPEN&minRiskScore=70&unassignedOnly=true&paymentId=PAY-REVIEW"
```

Supported filter parameters:
- `status`
- `assignee`
- `minRiskScore`
- `maxRiskScore`
- `breachedOnly`
- `unassignedOnly`
- `paymentId`
- `customerId`
- `createdFrom`
- `createdTo`

Supervisors can export the same filtered backlog as CSV:

```bash
curl -u senior.analyst:local-senior-2026 ^
  "http://localhost:8080/api/v1/fraud-cases/export?status=ESCALATED&breachedOnly=true&limit=1000"
```

Case exports default to 1,000 rows and reject HTTP limits outside `1..10000`. The service also enforces the 10,000-row maximum for non-HTTP callers and neutralizes spreadsheet-formula prefixes in exported text.

## Simulation Workflow

Use the simulation route when you want to test rules, tune thresholds, or demo outcomes without creating assessments, payments, or review cases:

```bash
curl -X POST http://localhost:8080/api/v1/fraud-assessments/simulations ^
  -u ingest.client:local-ingest-2026 ^
  -H "Content-Type: application/json" ^
  -d "{\
    \"paymentId\":\"PAY-SIM-2001\",\
    \"customerId\":\"CUST-2001\",\
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

The response shows the projected decision, projected payment status, velocity source, triggered factors, and whether a review case would be created.

## Threshold Comparison Workflow

Use this route when you want to compare the current production-style thresholds against a hypothetical tuning profile before changing configuration:

```bash
curl -X POST http://localhost:8080/api/v1/fraud-assessments/simulations/compare ^
  -u analyst.one:local-analyst-2026 ^
  -H "Content-Type: application/json" ^
  -d "{\
    \"scenario\":{\
      \"paymentId\":\"PAY-COMPARE-2001\",\
      \"customerId\":\"CUST-2001\",\
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

The response shows both the baseline outcome and the override outcome so analysts can see whether the decision, payment status, or review-case creation would change.

## Scoring Profile Workflow

Use the versioned profile endpoints when you want threshold changes to be reviewable and promotable instead of living only in ad hoc simulation inputs:

- create a draft scoring profile with `POST /api/v1/fraud-assessments/scoring-profiles`
- inspect the live profile with `GET /api/v1/fraud-assessments/scoring-profiles/active`
- compare a candidate profile against the live profile with `POST /api/v1/fraud-assessments/simulations/compare-saved-profile/{profileId}`
- activate the candidate once it is approved with `POST /api/v1/fraud-assessments/scoring-profiles/{profileId}/activate`

Profiles now version the complete rule definition as well as decision thresholds. Every persisted assessment captures the profile ID/version, ruleset version, normalized input snapshot, factor details, and request hash used for that decision.

## Outcome Feedback Workflow

Use the Decision Quality screen or `POST /api/v1/fraud-outcomes/assessments/{assessmentId}` to attach ground truth such as `CONFIRMED_FRAUD`, `ACCOUNT_TAKEOVER`, `CHARGEBACK`, `GENUINE`, `CUSTOMER_AUTHORIZED`, or `INCONCLUSIVE`. Conclusive labels feed precision, recall, false-positive rate, and net-loss metrics; inconclusive labels remain auditable without distorting model-quality calculations.

## Fraud Operations Summary

Use this route when analysts need a quick operations snapshot across assessments, payments, and review cases:

```bash
curl -u analyst.one:local-analyst-2026 http://localhost:8080/api/v1/fraud-operations/summary
```

The response includes:
- total assessments, tracked payments, and review cases
- distinct customers assessed
- average fraud risk score
- current review backlog count
- challenge pass, fail, abandonment, and pending metrics for threshold tuning
- reviewer workload, resolved output, backlog SLA pressure, drill-down, balancing recommendations, and case-priority routing hints
- outbound delivery counts for pending, delivered, and failed event dispatches
- grouped counts by decision, velocity source, payment status, case status, and resolution outcome

## Challenge Outcome Workflow

Use this route when a challenged payment completes step-up verification and you want that outcome to feed future tuning:

```bash
curl -X POST http://localhost:8080/api/v1/payments/PAY-CHALLENGE-1001/challenge-outcome ^
  -u analyst.one:local-analyst-2026 ^
  -H "Content-Type: application/json" ^
  -d "{\
    \"outcome\":\"PASSED\",\
    \"note\":\"Customer completed OTP challenge successfully.\"\
  }"
```

## Replay Workflow

Use this route when you want to run a named batch of historical or hypothetical scenarios and keep the results for later review:

```bash
curl -X POST http://localhost:8080/api/v1/fraud-replays ^
  -u analyst.one:local-analyst-2026 ^
  -H "Content-Type: application/json" ^
  -d "{\
    \"batchName\":\"July 17 historical replay\",\
    \"overrides\":{\
      \"challengeThreshold\":40,\
      \"holdThreshold\":60,\
      \"declineThreshold\":80\
    },\
    \"scenarios\":[\
      {\
        \"paymentId\":\"PAY-REPLAY-1001\",\
        \"customerId\":\"CUST-REPLAY-42\",\
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
      }\
    ]\
  }"
```

The replay response stores the batch metadata, applied thresholds, grouped decision counts, and itemized outcomes for every scenario.

Authenticated write operations now derive their audit actor from the authenticated HTTP Basic principal instead of trusting request-body identity fields.

Supervisor-grade commands now also require step-up verification. Generate a one-time token with `POST /api/v1/security/step-up/token`, verify it as the same authenticated operator, and then retry the protected command within the configured verification window.

The step-up delivery path is now environment-aware:

- the main local backend profile defaults to `EMAIL` delivery through Mailpit on `localhost:1025`
- test coverage still uses `DEVELOPMENT_LINK` so automated verification can stay deterministic
- the `prod` profile switches to `EMAIL` delivery and disables direct verification URL exposure
- every issue, resend, failure, expiry, revoke, and successful consume event is persisted in a delivery-audit table
- resending a token revokes any older open token for that operator
- consumed, revoked, expired, or failed tokens cannot be replayed

## Downstream Hooks

The platform now emits:
- assessment-completed events for fraud analytics consumers
- payment-status-changed events for downstream payment orchestration
- fraud-notifications events for challenge, hold, decline, review-case, and case-resolution notifications

These outbound events are persisted first and then dispatched from a delivery outbox so failed sends can retry without losing the fraud decision record.

Operators can now query recent outbound event records with richer filters, inspect failed deliveries, read retry and aging analytics, attach incident notes, export failed events for incident handling, requeue failed events, and trigger an immediate dispatch sweep from the fraud operations API.

## Live API Docs

Once the backend is running, OpenAPI documentation is available at:

- `http://localhost:8080/swagger-ui.html` with `platform.admin / local-admin-2026`
- `http://localhost:8080/v3/api-docs` with `platform.admin / local-admin-2026`
- `http://localhost:8080/v3/api-docs.yaml` with `platform.admin / local-admin-2026`

In the `prod` profile, Springdoc is disabled by default.

## Test Commands

```bash
cd backend
mvn test
mvn clean verify
```

When Docker is available, both commands run PostgreSQL 17 Testcontainers coverage that applies all Flyway migrations, validates Hibernate mappings, and exercises a real JPA repository. The PostgreSQL test is reported as skipped when Docker is unavailable; H2 remains the explicit fast-test fallback.

## Repository Metadata

Suggested public repository description:

`Production-style Spring Boot fraud decisioning platform with explainable scoring, review workflows, step-up security, Mailpit-backed local email delivery, Kafka/Redis/PostgreSQL integration, and OpenAPI docs.`

Suggested repository topics:

- `spring-boot`
- `java`
- `fraud-detection`
- `payment-platform`
- `kafka`
- `redis`
- `postgresql`
- `flyway`
- `swagger`
- `openapi`
- `docker-compose`
- `mailpit`

## Step-Up Security Workflow

Use the step-up endpoints when a supervisor or platform administrator is about to run a protected command such as profile activation, queue export, case release, decline confirmation, or outbound replay control.

Request a token:

```bash
curl -X POST http://localhost:8080/api/v1/security/step-up/token ^
  -u senior.analyst:local-senior-2026
```

In the default local setup, the response returns delivery metadata such as `deliveryChannel`, `destinationMasked`, `deliveryId`, and `expiresAt`, and the email itself is visible in Mailpit at `http://localhost:8025`. In test-oriented `DEVELOPMENT_LINK` mode, the response can still include a temporary `verificationUrl`.

Resend a token if the operator needs a fresh delivery:

```bash
curl -X POST http://localhost:8080/api/v1/security/step-up/token/resend ^
  -u senior.analyst:local-senior-2026
```

Verify the token in the same authenticated session:

```bash
curl -u senior.analyst:local-senior-2026 ^
  "http://localhost:8080/api/v1/security/step-up/verify?token={token}"
```

Read the delivery audit trail:

```bash
curl -u senior.analyst:local-senior-2026 ^
  "http://localhost:8080/api/v1/security/step-up/deliveries?limit=10"
```

Revoke outstanding tokens if the session should be reset:

```bash
curl -X POST http://localhost:8080/api/v1/security/step-up/revoke ^
  -u senior.analyst:local-senior-2026
```

## Roadmap

- add organization onboarding, invitations, plan limits, and admin user management
- add Stripe or Paddle billing for plans, trials, invoices, and usage limits
- integrate production email delivery with dedicated provider configuration and delivery observability
- add device, beneficiary, merchant, account, and IP relationship analysis for linked fraud-ring investigations
- publish small Java and TypeScript integration SDKs after the idempotency and outcome contracts stabilize
- add champion/challenger shadow scoring and automatic rollback criteria using the labelled outcome dataset
- consider anomaly or machine-learning scores only after enough trustworthy ground-truth labels exist

## Troubleshooting

- If migration tests fail locally after schema work, start Docker Desktop and rerun `mvn clean verify` so Flyway is validated against a clean PostgreSQL 17 container as well as H2.
- If Redis or Kafka are unavailable, the scoring API still starts, but local infrastructure-backed behavior is best tested with `docker compose up -d`.
- If a case cannot be released, inspect the original decision: a case created from an immediate `DECLINE` cannot be released by the current business rules.

## Grounding Principle

This project favors explainable, testable, and defensible fraud logic over fake complexity. The goal is to build something a senior engineer can reason about, extend, and demonstrate honestly.
