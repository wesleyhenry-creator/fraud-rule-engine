# Fraud Rule Engine

A backend service that receives categorised transaction events for insurance-linked banking products, applies a set of fraud rules targeting common insurance fraud patterns, stores each assessment, and exposes the results through a REST API.

Amounts are in ZAR. Business dates are evaluated in Africa/Johannesburg time.

## Stack

Java 17, Spring Boot 3.3, Spring Data JPA, PostgreSQL 16, Flyway, Micrometer/Prometheus, springdoc-openapi, JUnit 5, H2 (tests only).

## Quick start

The easiest way to run the service with its database:

```bash
docker compose up --build
```

The API is then available at `http://localhost:8080`. Swagger UI is at `http://localhost:8080/swagger-ui.html`.

To run only the container image, supply your own PostgreSQL:

```bash
docker build -t fraud-rule-engine .
docker run --rm -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/fraud \
  -e DB_USERNAME=fraud -e DB_PASSWORD=fraud \
  fraud-rule-engine
```

## Build and test

Requirements for local builds: JDK 17+ and Maven 3.9+.

```bash
mvn clean verify          # compiles, runs unit and integration tests
mvn spring-boot:run       # runs against localhost:5432 by default (see Configuration)
```

Tests use an in-memory H2 database in PostgreSQL mode, so no external services are needed. The Docker build also runs the full test suite; a failing test fails the image build.

Test layout:

- `FraudRulesTest` covers each rule's firing and non-firing boundaries.
- `FraudScoringEngineTest` covers score aggregation, the 100-point cap and decision thresholds.
- `FraudApiIntegrationTest` covers the HTTP contract end to end, including validation, idempotency, error mapping and a multi-transaction fraud sequence.

## API

Base path `/api/v1`. Errors use RFC 7807 `application/problem+json`.

| Method | Path | Purpose |
|---|---|---|
| POST | `/transactions` | Assess a transaction, store the event and assessment, return 201 |
| GET | `/assessments/{transactionId}` | Retrieve the assessment for a transaction |
| GET | `/assessments` | Search assessments with filters and pagination |
| GET | `/rules` | List active rules and their weights |

Submitting a transaction:

```bash
curl -s -X POST http://localhost:8080/api/v1/transactions \
  -H 'Content-Type: application/json' \
  -d '{
        "transactionId": "TX-1001",
        "policyNumber": "POL-88421",
        "accountId": "ACC-5530112",
        "payeeAccountId": "ACC-7712034",
        "category": "CLAIM_PAYOUT",
        "amount": 60000.00,
        "occurredAt": "2026-10-10T10:00:00Z",
        "claimReference": "CLM-2026-0091",
        "policyInceptionDate": "2026-10-01"
      }'
```

Search filters on `GET /assessments`: `decision`, `riskLevel`, `policyNumber`, `from`, `to` (ISO-8601 instants on `evaluatedAt`), `page` (default 0), `size` (default 20, max 100). Results are ordered newest first.

Example:

```bash
curl -s 'http://localhost:8080/api/v1/assessments?decision=FLAG_FOR_INVESTIGATION&size=10'
```

Categories: `PREMIUM_PAYMENT`, `CLAIM_SUBMISSION`, `CLAIM_PAYOUT`, `BENEFICIARY_UPDATE`, `POLICY_CHANGE`.

Decisions: `CLEAR` (score below 40), `REFER_FOR_REVIEW` (40 to 69), `FLAG_FOR_INVESTIGATION` (70 and above). Risk levels map one-to-one: `LOW`, `MEDIUM`, `HIGH`.

Submitting the same `transactionId` twice returns `409 Conflict`. The uniqueness check is also enforced by a database constraint, so concurrent duplicates are rejected safely.

## Fraud rules

Each rule adds its weight to the risk score when it fires. The score is capped at 100.

| Code | Weight | Fires when |
|---|---|---|
| `HIGH_VALUE_CLAIM` | 25 | A claim is at or above R50,000 |
| `CLAIM_VELOCITY` | 30 | A policy has more than 3 claims in a rolling 30 days |
| `ROUND_AMOUNT_CLAIM` | 10 | A claim of R10,000 or more is an exact multiple of R1,000 |
| `EARLY_CLAIM` | 20 | A claim is lodged within 30 days of policy inception |
| `RECENT_BENEFICIARY_CHANGE_PAYOUT` | 25 | A payout goes to an account nominated as beneficiary within the last 14 days |
| `DUPLICATE_CLAIM` | 35 | A claim repeats a claim reference, or the same amount and payee, within 7 days |

The rules reflect common insurance fraud typologies: inflated or fabricated claims, claim farming, early-life claims on new policies, account takeover followed by a redirected payout, and resubmitted claims.

Rules are evaluated against the policy's prior transactions from the last 90 days. Each rule is a stateless Spring bean implementing `FraudRule`, so adding a rule means adding one class; the scoring engine picks it up automatically.

## Configuration

All thresholds and weights live in `application.yml` and can be overridden with environment variables, for example:

```bash
FRAUD_HIGH_VALUE_CLAIM_THRESHOLD=75000.00 FRAUD_INVESTIGATE_THRESHOLD=80 mvn spring-boot:run
```

Settings are validated at startup. For instance, the review threshold must not exceed the investigate threshold, and rule windows must fit within the 90-day history lookback. An invalid value stops the service from starting, rather than producing incorrect decisions.

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/fraud` | JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `fraud` / `fraud` | Database credentials, local defaults only |
| `DB_POOL_SIZE` | `10` | Hikari maximum pool size |
| `PORT` | `8080` | HTTP port |
| `JAVA_OPTS` | `-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError` | JVM options (Docker image) |

Rule parameters are listed in `application.yml` under the `fraud` prefix.

## Operations

- Health: `GET /actuator/health` (liveness and readiness probes are enabled).
- Metrics: `GET /actuator/prometheus`. The counter `fraud.assessments` is tagged by `decision` and `risk_level`.
- Logs record the transaction id, decision and score. Account identifiers are masked in rule reasons and never logged.
- Schema is managed by Flyway (`src/main/resources/db/migration`). Hibernate runs in `validate` mode, so the service refuses to start if the schema drifts from the entities.

## Design notes

- Rules and scoring are separated from persistence and HTTP. The engine is pure Java and is unit tested without Spring.
- Each assessment stores the rule version, the rules that fired and their reasons, so a decision can be explained and audited later.
- Transactions are assessed synchronously and persisted in a single database transaction, so a stored event always has an assessment.
- Account numbers are masked in reasons and responses, consistent with POPIA data minimisation.

## Known limitations and next steps

These are deliberately out of scope for this iteration and would be the next priorities for production:

- **Authentication and authorisation.** The API is currently unauthenticated. It should sit behind an API gateway with OAuth2/JWT or mTLS, with separate read and write scopes.
- **Ingestion.** Events arrive over REST. For high volume, a Kafka consumer with the same service layer would be more suitable, and batch submission would reduce round trips.
- **Data protection.** Account and policy identifiers are stored in plain text. Field-level encryption or tokenisation should be applied before production use.
- **Rule governance.** Rule weights are configuration, not versioned rules. A production system would add rule-level backtesting against labelled fraud outcomes and a review workflow for changes.
- **Case management.** `REFER_FOR_REVIEW` and `FLAG_FOR_INVESTIGATION` results are stored but not yet routed to an investigator queue.
- **Currency.** Only ZAR is supported.
