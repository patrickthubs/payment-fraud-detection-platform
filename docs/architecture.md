# Architecture

## Current Architecture

As of Saturday, July 18, 2026, this repository runs as a production-oriented modular monolith with a separate Angular operator console:

- the complete local platform can run as a six-service Docker Compose project
- the backend is packaged as a non-root Java 26 runtime image
- the Angular production bundle is served by Nginx with same-origin API proxying
- PostgreSQL and Redis data use named Docker volumes
- service logs remain on stdout/stderr for Docker Desktop inspection with rotation

- `backend`: one Spring Boot 4 service
- `ui`: one Angular application
- `docker-compose.yml`: local PostgreSQL, Redis, Kafka, and Mailpit

This is the current architecture of the project today. The repository does not currently run as multiple backend microservices.

## Problem Framing

Banks and payment processors detect suspicious payments by combining many signals quickly enough to decide whether a payment should be:

- allowed
- challenged
- held
- declined

The engineering challenge is not only scoring. It is explainable, low-latency, operationally safe decisioning with durable review, audit, and recovery behavior around that score.

## Why The Current Shape Works

- keeps the synchronous decision path simple
- keeps fraud logic explainable and testable
- keeps durable business state in PostgreSQL
- uses Redis for low-latency velocity features
- uses Kafka for asynchronous fraud and payment event distribution
- gives operators a separate UI without leaking backend business logic into the frontend
- avoids premature microservice overhead while still maintaining clean internal boundaries

## Current Deployable Units

### Backend

The backend is one deployable Spring Boot service with conventional package boundaries:

- `web`
- `service`
- `repository`
- `entity`
- `dto`
- `model`
- `config`
- `exception`

### UI

The UI is a separate Angular application that calls the backend APIs and exposes operator workflows through routeable screens instead of one oversized dashboard.

### Local Infrastructure

The local stack is provided through Docker Compose:

- PostgreSQL on `localhost:15432`
- Redis on `localhost:16379`
- Kafka on `localhost:19092`
- Mailpit SMTP on `localhost:1025`
- Mailpit web UI on `http://localhost:8025`

## Functional Boundaries In The Current Backend

The application is one deployable service, but it is already split into meaningful backend capabilities.

### Assessment and Scoring

Owns:

- fraud assessment intake
- risk factor evaluation
- score calculation
- decision selection
- explanation summary creation

Representative code:

- `web/FraudAssessmentController`
- `service/FraudAssessmentService`
- `service/FraudRiskScoringService`
- `service/RedisVelocityFeatureService`

### Payment Lifecycle

Owns:

- persisted payment records
- payment status transitions
- challenge outcome completion
- payment query APIs

Representative code:

- `web/PaymentController`
- `service/PaymentLifecycleService`
- `service/PaymentQueryService`

### Fraud Case Management

Owns:

- review queue filters
- case assignment
- notes
- escalation
- release and decline confirmation
- case timeline persistence

Representative code:

- `web/FraudCaseController`
- `service/FraudCaseQueryService`
- `service/FraudCaseCommandService`

### Replay and Simulation

Owns:

- non-persistent simulations
- threshold comparisons
- persisted replay batches
- scoring profile management
- complete rule-definition versioning and decision provenance
- confirmed outcome labelling and quality measurement

Representative code:

- `web/FraudReplayBatchController`
- `service/FraudSimulationService`
- `service/FraudReplayBatchService`
- `service/FraudScoringProfileService`

### Outbound Operations

Owns:

- durable outbound event persistence
- retry and dispatch operations
- failed event visibility
- incident notes
- operational summaries

Representative code:

- `web/FraudOperationsController`
- `service/FraudOutboundEventService`
- `service/FraudOutboundEventDispatcher`
- `service/FraudOutboundEventOperationsService`
- `service/FraudOutboundAnalyticsService`
- `service/FraudOperationsSummaryService`

### Security and Operator Access

Owns:

- persisted operators and roles
- organization membership for tenant-aware operator sessions
- server-side browser sessions with CSRF protection
- OAuth2 JWT bearer authentication for production machine clients
- HTTP Basic authentication restricted to local and test configuration
- step-up token generation
- delivery audit
- resend, revoke, and verification flows

Representative code:

- `config/SecurityConfiguration`
- `web/StepUpAuthenticationController`
- `service/DatabaseFraudOperatorDetailsService`
- `service/StepUpAuthenticationService`
- `service/StepUpDeliveryGatewayImpl`

## Current UI Surface

The Angular UI is not a static demo. It is an operator console for the backend capabilities above.

Current route areas:

- login
- command center
- overview
- cases
- payments
- rules studio
- simulations
- decision quality
- operations

The UI stays table-first for operational workflows and uses routeable pages so command-center triage, queue review, payment inspection, rules work, simulation work, decision-quality labelling, and operational recovery do not collapse into one long screen.

The Command Center is the default day-to-day operator surface. It combines backlog pressure, active policy posture, priority cases, held payments, and quality signals into one actionable screen. The Risk Rules Studio is separated because threshold/rule promotion is a supervisor workflow with higher blast radius than normal case review.

## Data Ownership and Infrastructure

### PostgreSQL

PostgreSQL is the system of record for:

- fraud assessments
- fraud cases
- case timeline entries
- payment records
- payment state transitions
- replay batches and replay items
- scoring profiles
- outbound delivery records
- operators and roles
- organizations and operator organization membership
- step-up delivery audit and operator security state
- fraud outcome labels, monetary loss, and recovery evidence

Schema evolution is handled through Flyway migrations under `backend/src/main/resources/db/migration`.

### Organization Boundary

`fraud_organizations` is the commercial SaaS tenant anchor. Operators belong to one organization, authenticated sessions expose that organization context to the Angular console, and core fraud/payment records carry an `organization_id` tenant key.

The platform uses a shared-database tenant-column model. Assessments, review cases, timeline entries, payments, payment transitions, scoring profiles, outcomes, replay batches, replay items, outbound events, step-up delivery audit rows, and step-up operator security state rows are tenant-owned. Operator-facing service/repository paths scope access to the current organization. Background outbound dispatch remains global because it is an internal worker flow.

Remaining hardening before paid multi-customer hosting: add self-service organization onboarding, tenant administration, plan limits, billing, and production delivery observability.

### Redis

Redis supports low-latency velocity and short-lived risk features in the scoring path.

Typical responsibilities:

- recent transaction windows
- short-lived counters
- customer or device risk hints
- short-lived replay protection style checks

### Kafka

Kafka is used for asynchronous fraud and payment events. The synchronous fraud decision path does not depend on Kafka availability to return the immediate API response.

Current event-oriented responsibilities include:

- publishing fraud assessment outcomes
- publishing payment status changes
- feeding outbound notification and orchestration workflows

### Mailpit

Mailpit supports local step-up verification email delivery so privileged operations can be tested without a paid external email provider.

## Core Request Flows

### Fraud assessment flow

1. An authenticated client submits a payment risk assessment.
2. The backend gathers request features and velocity context.
3. The scoring service computes factors, score, and decision using one immutable profile snapshot.
4. The backend persists the assessment result together with its profile/ruleset version, normalized inputs, factors, request hash, and optional idempotency key.
5. If required, it creates or updates payment and case state.
6. It records outbound events for asynchronous dispatch.
7. The API returns an explainable decision response immediately.

### Supervisor step-up flow

1. A protected operator requests a step-up token.
2. The backend persists delivery intent and security state.
3. A verification email is sent through Mailpit in local development.
4. The operator verifies the token or verification link.
5. The backend marks elevated session state and allows protected actions until expiry.

Step-up delivery audit and lockout state are stored with `organization_id`, so delivery listing, resend, revoke, verification, and rate-limit behavior stay inside the authenticated tenant boundary.

### Outcome feedback flow

1. A supervisor records confirmed ground truth for an assessment from customer confirmation, chargeback, investigation, or another evidence source.
2. The backend stores the label, loss, recovery, evidence source, operator, and timestamps with optimistic concurrency protection.
3. Decision-quality analytics join conclusive labels to their original immutable decisions.
4. Precision, recall, false-positive rate, and net loss become the evidence used for future ruleset promotion.

## Failure Handling

### Redis unavailable

The service can still score using request-carried features and persisted configuration, but may lose some low-latency enrichment fidelity.

### Kafka unavailable

The service still returns the synchronous fraud decision, while outbound events remain durably tracked for retry and operational recovery.

### Mail delivery unavailable

Step-up generation fails visibly and remains auditable through delivery records and operator-facing troubleshooting.

## Security and Privacy

- operator accounts are persisted in the database, not kept in memory
- operator sessions include organization context, and operator-facing business data access is tenant-scoped
- sensitive privileged actions require step-up verification
- case, payment, and outbound actions are auditable
- generated references are preferred over sensitive payment instrument data
- secrets and raw verification tokens should not be treated as loggable business data
- browser operators use server-side sessions protected by CSRF tokens; passwords are never persisted in browser storage
- known demo identities are removed by migration and recreated only when the explicit `local` profile enables demo users
- production datasource credentials have no committed fallback values
- production machine tokens are validated against a configured issuer and audience, with explicit JWT role-claim mapping
- production machine tokens must include a UUID tenant claim, `organization_id` by default, before tenant-scoped service paths can read or write data
- fraud case CSV exports are database-bounded to at most 10,000 rows

## Observability

The current backend exposes operational summary views through its APIs, including:

- platform assessment counts
- review backlog
- payment lifecycle state
- outbound delivery health
- replay posture
- step-up delivery audit
- reviewer and queue analytics
- labelled decision quality, false-positive pressure, and fraud-loss recovery

## Testing Strategy

The current repository is validated through:

- unit tests for core services and scoring rules
- Spring Boot application test coverage
- PostgreSQL 17 Testcontainers coverage for all Flyway migrations, Hibernate validation, and repository behavior
- Maven verification through `mvn test` and `mvn clean verify`
- local smoke testing through `docs/smoke-test.md`
- Angular production build validation for the UI

## What This Architecture Is Not

- not a deployed microservice mesh
- not a card network simulator
- not a bank-core payment switch
- not a machine-learning fraud platform
- not yet a fully production-onboarded commercial SaaS with self-service tenant onboarding, billing, and plan enforcement

It is a production-oriented modular monolith that demonstrates real fraud decisioning patterns, review workflows, outbound recovery, reviewer operations, organization-aware operator access, and step-up operator security with a usable UI.
