package com.capitecbank.fraud.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "fraud_assessment_rule")
public class FraudAssessmentRule {

    public static final int MAX_REASON_LENGTH = 500;

    @EmbeddedId
    private FraudAssessmentRuleId id;

    @MapsId("assessmentId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private FraudAssessment assessment;

    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;

    @Column(name = "weight", nullable = false)
    private int weight;

    @Column(name = "reason", nullable = false, length = MAX_REASON_LENGTH)
    private String reason;

    protected FraudAssessmentRule() {
    }

    FraudAssessmentRule(FraudAssessment assessment, int ruleOrder, String ruleCode, int weight, String reason) {
        this.id = new FraudAssessmentRuleId(assessment.getId(), ruleOrder);
        this.assessment = assessment;
        this.ruleCode = ruleCode;
        this.weight = weight;
        this.reason = reason.length() > MAX_REASON_LENGTH ? reason.substring(0, MAX_REASON_LENGTH) : reason;
    }
}
