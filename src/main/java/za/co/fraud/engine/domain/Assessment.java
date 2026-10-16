package za.co.fraud.engine.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** The stored result of evaluating one transaction against the fraud rule set. */
public record Assessment(
        UUID id,
        TransactionEvent transaction,
        int riskScore,
        RiskLevel riskLevel,
        Decision decision,
        List<RuleOutcome> triggeredRules,
        String rulesVersion,
        Instant evaluatedAt) {
}
