# Architecture

## Problem Framing

Banks and payment processors detect suspicious payments by combining many signals quickly enough to decide whether a payment should be:
- allowed
- challenged
- held
- declined

The main engineering challenge is not just classification. It is low-latency, explainable, operationally safe decisioning under partial information.

## What Is Good About The Proposed Direction

- Uses boring, reliable infrastructure for the core path
- Keeps fraud logic explainable before jumping to opaque ML
- Separates real-time scoring from longer-running case review
- Uses Redis where sub-millisecond counters matter
- Uses Kafka where event ordering and asynchronous fan-out matter
- Leaves PostgreSQL for durable state, analyst review, and audit

## What Is Risky

- Premature microservices can create operational overhead before the rules are stable
- Real fraud scoring can become an untestable rule pile if rule ownership is unclear
- False positives can damage customer trust if the system only optimizes for fraud catch rate
- Real-time dependencies can create decision latency if every feature lookup becomes synchronous

## What Is Missing In Many Naive Designs

- explicit latency budgets
- decision explainability
- rule simulation and replay
- analyst case-management workflow
- event idempotency
- partial-failure strategy when Redis or Kafka is degraded
- measurement of false-positive cost

## Simpler Alternative

A simpler first version is a modular monolith:
- one decisioning service
- stateless scoring endpoint
- externalized interfaces for Kafka and Redis
- one local Docker stack

That is the recommended starting point for this project.

## Recommended Architecture

### Phase 1

Single Spring Boot service:
- `fraud assessment API`
- `rules-based scoring engine`
- `velocity feature abstraction`
- `decision explanation builder`

Infrastructure present but lightly coupled:
- Kafka for asynchronous fraud and payment events
- Redis for future feature lookups
- PostgreSQL for case data, replay batches, payment state, and outbound delivery records

### Phase 2

Split into bounded services when the behavior stabilizes:
- `payment-intake-service`
- `fraud-decision-service`
- `case-management-service`
- `audit-ingest-service`

## Domain Boundaries

### Payment Intake

Owns:
- payment submission requests
- payment identifiers
- routing into fraud decisioning

Does not own:
- final analyst workflow
- device intelligence reference data

### Fraud Decisioning

Owns:
- risk scoring
- rule evaluation
- decision outcome
- decision explanation

Does not own:
- customer ledger movement
- final settlement orchestration

### Case Management

Owns:
- manual review queues
- analyst decisions
- disposition notes
- escalation workflow

## Real-Time Decision Model

Recommended actions:
- `ALLOW`
- `CHALLENGE`
- `HOLD`
- `DECLINE`

Suggested meaning:
- `ALLOW`: low enough risk to continue automatically
- `CHALLENGE`: require OTP or stronger customer step-up
- `HOLD`: stop automated completion and send to review
- `DECLINE`: reject immediately

## Example Fraud Signals

Signals to model early:
- amount compared to customer baseline
- transaction count in last 5 minutes
- spend in last 1 hour
- new device
- impossible travel
- beneficiary age
- merchant risk class
- recent account security changes
- country risk

These are realistic categories without claiming access to proprietary banking signals.

## Redis Usage

Redis is appropriate for:
- short-lived counters
- velocity windows
- recent device keys
- recent beneficiary keys
- idempotency guards

Examples:
- `velocity:customer:{customerId}:5m`
- `velocity:device:{deviceId}:5m`
- `beneficiary:new:{customerId}:{beneficiaryId}`

## Kafka Usage

Kafka is appropriate for:
- `payment-submitted`
- `fraud-assessment-completed`
- `payment-held`
- `payment-declined`
- `case-created`

Why Kafka here:
- decouples scoring from downstream consumers
- supports replay for analytics and rule tuning
- gives clean event boundaries for future service separation

## PostgreSQL Usage

PostgreSQL should store:
- fraud cases
- analyst notes
- disposition history
- rule versions
- replay batches
- audit metadata

Avoid putting ultra-hot velocity reads on PostgreSQL in the real-time path.

## Latency Target

Initial target:
- p95 decision latency below 150 ms for the scoring API

Why:
- keeps the system usable for synchronous payment checks
- realistic for a rules-first engine with cached features

## Failure Modes

### Redis unavailable

Fallback:
- degrade to request-carried features
- mark assessment confidence lower
- optionally shift medium-risk outcomes from `ALLOW` to `CHALLENGE`

### Kafka unavailable

Fallback:
- return synchronous decision
- persist outbound payloads in PostgreSQL
- retry delivery from the outbox on a scheduled dispatcher
- expose failed delivery counts in operations reporting

### Rule misconfiguration

Mitigation:
- rule versioning
- replay tests
- safe rollout flags

## Security And Privacy

- never log full PAN-like data
- never log secrets or tokens
- prefer generated payment references over sensitive account numbers
- log why a payment was challenged or held, but not sensitive credentials
- keep manual review actions auditable

## Observability

Metrics to expose:
- total assessments
- decision counts by action
- score distribution
- challenge rate
- hold rate
- decline rate
- latency p50/p95/p99
- feature fallback rate
- outbound event pending count
- outbound event failed count

Structured logs should include:
- paymentId
- customerId
- decision
- score
- triggered factor codes

## Testing Strategy

- unit tests for scoring rules
- contract tests for API requests/responses
- integration tests when Kafka/Redis are bound for real
- replay-style tests for historical fraud scenarios later

## Current Implementation Strategy

This repo starts with a single decisioning service because it is the safest way to validate:
- signal selection
- scoring semantics
- API contracts
- explainability

Only after those stabilize should we split into multiple deployable services.
