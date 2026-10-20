package com.capitecbank.fraud.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
@Embeddable
public class FraudAssessmentRuleId implements Serializable {

    @Column(name = "assessment_id", nullable = false)
    private UUID assessmentId;

    @Column(name = "rule_order", nullable = false)
    private int ruleOrder;

    protected FraudAssessmentRuleId() {
    }

    public FraudAssessmentRuleId(UUID assessmentId, int ruleOrder) {
        this.assessmentId = assessmentId;
        this.ruleOrder = ruleOrder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FraudAssessmentRuleId that)) {
            return false;
        }
        return ruleOrder == that.ruleOrder && Objects.equals(assessmentId, that.assessmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assessmentId, ruleOrder);
    }
}
