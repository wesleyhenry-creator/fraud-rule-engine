package za.co.fraud.engine.api.dto;

import za.co.fraud.engine.domain.Assessment;
import za.co.fraud.engine.domain.Decision;
import za.co.fraud.engine.domain.RiskLevel;
import za.co.fraud.engine.domain.TransactionCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssessmentResponse(
        UUID assessmentId,
        String transactionId,
        String policyNumber,
        TransactionCategory category,
        BigDecimal amount,
        int riskScore,
        RiskLevel riskLevel,
        Decision decision,
        List<TriggeredRuleResponse> triggeredRules,
        String rulesVersion,
        Instant evaluatedAt) {

    public static AssessmentResponse from(Assessment assessment) {
        return new AssessmentResponse(
                assessment.id(),
                assessment.transaction().transactionId(),
                assessment.transaction().policyNumber(),
                assessment.transaction().category(),
                assessment.transaction().amount(),
                assessment.riskScore(),
                assessment.riskLevel(),
                assessment.decision(),
                assessment.triggeredRules().stream()
                        .map(r -> new TriggeredRuleResponse(r.ruleCode(), r.weight(), r.reason()))
                        .toList(),
                assessment.rulesVersion(),
                assessment.evaluatedAt());
    }
}
