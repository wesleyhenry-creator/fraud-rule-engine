# Entity Relationship Diagram: Fraud Rule Engine

Derived from `src/main/resources/db/migration/V1__create_fraud_schema.sql`.

```mermaid
erDiagram
    TRANSACTION_EVENT {
        bigint id PK
        varchar transaction_id UK "business id, max 64"
        varchar policy_number "indexed with occurred_at"
        varchar account_id "policyholder account, masked in output"
        varchar payee_account_id "payout or new beneficiary account"
        varchar category "PREMIUM_PAYMENT, CLAIM_SUBMISSION, CLAIM_PAYOUT, BENEFICIARY_UPDATE, POLICY_CHANGE"
        numeric amount "15,2 ZAR"
        timestamptz occurred_at
        varchar claim_reference "nullable"
        date policy_inception_date "nullable"
        timestamptz received_at
    }

    FRAUD_ASSESSMENT {
        uuid id PK
        bigint transaction_event_id FK "UK, one assessment per event"
        int risk_score "0 to 100"
        varchar risk_level "LOW, MEDIUM, HIGH"
        varchar decision "CLEAR, REFER_FOR_REVIEW, FLAG_FOR_INVESTIGATION"
        varchar rules_version
        timestamptz evaluated_at "indexed with decision and risk_level"
    }

    FRAUD_ASSESSMENT_RULE {
        uuid assessment_id PK, FK
        int rule_order PK "evaluation order"
        varchar rule_code
        int weight
        varchar reason "500 chars, masked accounts only"
    }

    TRANSACTION_EVENT ||--|| FRAUD_ASSESSMENT : "is assessed by"
    FRAUD_ASSESSMENT ||--o{ FRAUD_ASSESSMENT_RULE : "records rules fired"
```

## Relationships

- **TRANSACTION_EVENT to FRAUD_ASSESSMENT (one to one).** Each event is assessed exactly once. The event and its assessment are written in the same database transaction, so every stored event has an assessment. The unique constraint on `transaction_event_id` enforces the one-to-one rule.
- **FRAUD_ASSESSMENT to FRAUD_ASSESSMENT_RULE (one to many).** An assessment records zero or more fired rules. `rule_order` keeps evaluation order and forms part of the composite primary key.

## Notes

- `policy_number` is not a foreign key. The service has no policy table. Policy history is read by querying `transaction_event` by policy number and a 90-day window.
- `transaction_id` is the business key used by the API. `transaction_event.id` is the internal surrogate key.
- `FRAUD_ASSESSMENT_RULE` is an element collection owned by `FRAUD_ASSESSMENT`, so rule rows are deleted or created only through their parent.
