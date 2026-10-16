package za.co.fraud.engine.scoring;

import za.co.fraud.engine.domain.Decision;
import za.co.fraud.engine.domain.RiskLevel;
import za.co.fraud.engine.domain.RuleOutcome;

import java.util.List;

public record ScoringResult(int riskScore, RiskLevel riskLevel, Decision decision, List<RuleOutcome> triggeredRules) {
}
