package com.capitecbank.fraud.api;

import com.capitecbank.fraud.domain.Decision;
import com.capitecbank.fraud.domain.RiskLevel;
import com.capitecbank.fraud.domain.TransactionCategory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AssessmentResponse(
        String transactionId,
        String policyNumber,
        String accountId,
        String payeeAccountId,
        TransactionCategory category,
        BigDecimal amount,
        Instant occurredAt,
        UUID assessmentId,
        int riskScore,
        RiskLevel riskLevel,
        Decision decision,
        String rulesVersion,
        Instant evaluatedAt,
        List<RuleHitResponse> rules) {
}
