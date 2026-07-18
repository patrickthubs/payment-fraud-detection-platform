# Smoke Test Guide

This guide is for someone who just cloned the project and wants to prove the platform works locally without reading the entire API workflow document first.

## Prerequisites

- Java 26
- Maven 3.9+
- Docker Desktop or a compatible Docker runtime

## 1. Clone The Project

```bash
git clone <repo-url>
cd payment-fraud-detection-platform
```

## 2. Start Infrastructure

```bash
docker compose up -d postgres redis kafka mailpit
```

Expected local services:

- PostgreSQL on `localhost:15432`
- Redis on `localhost:16379`
- Kafka on `localhost:19092`
- Mailpit SMTP on `localhost:1025`
- Mailpit web UI on `http://localhost:8025`

## 3. Start The Backend

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The API should come up on `http://localhost:8080`.

## 4. Confirm The Service Is Alive

```bash
curl http://localhost:8080/actuator/health
```

Expected result:

- HTTP `200`
- health payload showing the app is up

## 5. Sign In Through Swagger

Open:

- `http://localhost:8080/swagger-ui.html`

Use:

- `platform.admin / local-admin-2026`

## 6. Submit A Smoke-Test Assessment

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

Expected result:

- HTTP `200`
- a persisted decision
- a likely `reviewCaseId`

## 7. Inspect Queue And Payment State

```bash
curl -u analyst.one:local-analyst-2026 http://localhost:8080/api/v1/fraud-cases
curl -u analyst.one:local-analyst-2026 http://localhost:8080/api/v1/payments/PAY-SMOKE-1001
```

Look for:

- the payment status
- the risk decision
- the review case timeline

## 8. Validate Step-Up Security

Request a supervisor step-up token:

```bash
curl -X POST http://localhost:8080/api/v1/security/step-up/token ^
  -u senior.analyst:local-senior-2026
```

Then:

1. Open `http://localhost:8025`
2. Open the latest email for `senior.analyst@internal.local`
3. Click the verification link
4. Retry a protected endpoint, for example:

```bash
curl -u senior.analyst:local-senior-2026 ^
  "http://localhost:8080/api/v1/fraud-cases/export?paymentId=PAY-SMOKE-1001"
```

Expected result:

- before verification, protected supervisor endpoints return `403`
- after verification, the same protected endpoint succeeds

## 9. Run Automated Verification

```bash
cd backend
mvn test
mvn clean verify
```

Expected result:

- both commands complete successfully

## Troubleshooting

- If the backend cannot connect to infrastructure, rerun `docker compose up -d` and confirm the ports above are free.
- If step-up emails do not appear, refresh Mailpit at `http://localhost:8025`.
- If a protected endpoint still returns `403`, request a fresh token and verify it in the same authenticated session.
