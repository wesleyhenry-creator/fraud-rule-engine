package za.co.fraud.engine.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import za.co.fraud.engine.domain.TransactionCategory;
import za.co.fraud.engine.domain.TransactionEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "transaction_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransactionEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, length = 64)
    private String transactionId;

    @Column(name = "policy_number", nullable = false, length = 40)
    private String policyNumber;

    @Column(name = "account_id", nullable = false, length = 40)
    private String accountId;

    @Column(name = "payee_account_id", nullable = false, length = 40)
    private String payeeAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TransactionCategory category;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "claim_reference", length = 64)
    private String claimReference;

    @Column(name = "policy_inception_date")
    private LocalDate policyInceptionDate;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    public static TransactionEventEntity fromDomain(TransactionEvent event, Instant receivedAt) {
        TransactionEventEntity entity = new TransactionEventEntity();
        entity.transactionId = event.transactionId();
        entity.policyNumber = event.policyNumber();
        entity.accountId = event.accountId();
        entity.payeeAccountId = event.payeeAccountId();
        entity.category = event.category();
        entity.amount = event.amount();
        entity.occurredAt = event.occurredAt();
        entity.claimReference = event.claimReference();
        entity.policyInceptionDate = event.policyInceptionDate();
        entity.receivedAt = receivedAt;
        return entity;
    }

    public TransactionEvent toDomain() {
        return new TransactionEvent(transactionId, policyNumber, accountId, payeeAccountId, category,
                amount, occurredAt, claimReference, policyInceptionDate);
    }
}
