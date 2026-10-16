package za.co.fraud.engine.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import za.co.fraud.engine.domain.Assessment;
import za.co.fraud.engine.domain.Decision;
import za.co.fraud.engine.domain.RiskLevel;
import za.co.fraud.engine.scoring.ScoringResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "fraud_assessment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FraudAssessmentEntity {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_event_id", nullable = false, unique = true)
    private TransactionEventEntity event;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 10)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Decision decision;

    @Column(name = "rules_version", nullable = false, length = 20)
    private String rulesVersion;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "fraud_assessment_rule", joinColumns = @JoinColumn(name = "assessment_id"))
    @OrderColumn(name = "rule_order")
    private List<TriggeredRuleEntity> triggeredRules = new ArrayList<>();

    public static FraudAssessmentEntity create(TransactionEventEntity event, ScoringResult result,
                                               String rulesVersion, Instant evaluatedAt) {
        FraudAssessmentEntity entity = new FraudAssessmentEntity();
        entity.id = UUID.randomUUID();
        entity.event = event;
        entity.riskScore = result.riskScore();
        entity.riskLevel = result.riskLevel();
        entity.decision = result.decision();
        entity.rulesVersion = rulesVersion;
        entity.evaluatedAt = evaluatedAt;
        result.triggeredRules().forEach(r -> entity.triggeredRules.add(TriggeredRuleEntity.fromDomain(r)));
        return entity;
    }

    public Assessment toDomain() {
        return new Assessment(
                id,
                event.toDomain(),
                riskScore,
                riskLevel,
                decision,
                triggeredRules.stream().map(TriggeredRuleEntity::toDomain).toList(),
                rulesVersion,
                evaluatedAt);
    }
}
