package za.co.fraud.engine.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import za.co.fraud.engine.domain.RuleOutcome;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TriggeredRuleEntity {

    @Column(name = "rule_code", nullable = false, length = 60)
    private String ruleCode;

    @Column(nullable = false)
    private int weight;

    @Column(nullable = false, length = 500)
    private String reason;

    public TriggeredRuleEntity(String ruleCode, int weight, String reason) {
        this.ruleCode = ruleCode;
        this.weight = weight;
        this.reason = reason;
    }

    public static TriggeredRuleEntity fromDomain(RuleOutcome outcome) {
        return new TriggeredRuleEntity(outcome.ruleCode(), outcome.weight(), outcome.reason());
    }

    public RuleOutcome toDomain() {
        return new RuleOutcome(ruleCode, weight, reason);
    }
}
