package com.capitecbank.fraud.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;

@Getter
@Entity
@Table(name = "transaction_event",
        indexes = @Index(name = "ix_transaction_event_policy_occurred", columnList = "policy_number, occurred_at"))
public class TransactionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 64)
    private String transactionId;

    @Column(name = "policy_number", nullable = false, length = 50)
    private String policyNumber;

    @Column(name = "account_id", nullable = false, length = 50)
    private String accountId;

    @Column(name = "payee_account_id", length = 50)
    private String payeeAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private TransactionCategory category;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "claim_reference", length = 64)
    private String claimReference;

    @Column(name = "policy_inception_date")
    private LocalDate policyInceptionDate;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected TransactionEvent() {
    }

    public TransactionEvent(String transactionId, String policyNumber, String accountId, String payeeAccountId,
                            TransactionCategory category, BigDecimal amount, Instant occurredAt,
                            String claimReference, LocalDate policyInceptionDate, Instant receivedAt) {
        this.transactionId = transactionId;
        this.policyNumber = policyNumber;
        this.accountId = accountId;
        this.payeeAccountId = payeeAccountId;
        this.category = category;
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.occurredAt = occurredAt;
        this.claimReference = claimReference;
        this.policyInceptionDate = policyInceptionDate;
        this.receivedAt = receivedAt;
    }
}
