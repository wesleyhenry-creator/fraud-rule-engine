package com.capitecbank.fraud.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.annotations.BatchSize;

@Getter
@Entity
@Table(name = "fraud_assessment",
        indexes = @Index(name = "ix_fraud_assessment_decision_risk_evaluated",
                columnList = "decision, risk_level, evaluated_at"))
public class FraudAssessment {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_event_id", nullable = false, unique = true)
    private TransactionEvent transactionEvent;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 16)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 32)
    private Decision decision;

    @Column(name = "rules_version", nullable = false, length = 32)
    private String rulesVersion;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    @OneToMany(mappedBy = "assessment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id.ruleOrder ASC")
    @BatchSize(size = 100)
    private List<FraudAssessmentRule> rules = new ArrayList<>();

    protected FraudAssessment() {
    }

    public FraudAssessment(TransactionEvent transactionEvent, int riskScore, RiskLevel riskLevel,
                           Decision decision, String rulesVersion, Instant evaluatedAt) {
        this.id = UUID.randomUUID();
        this.transactionEvent = transactionEvent;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.decision = decision;
        this.rulesVersion = rulesVersion;
        this.evaluatedAt = evaluatedAt;
    }

    public void addRule(int ruleOrder, String ruleCode, int weight, String reason) {
        rules.add(new FraudAssessmentRule(this, ruleOrder, ruleCode, weight, reason));
    }
}
