package com.capitecbank.fraud.engine;

import com.capitecbank.fraud.domain.Decision;
import com.capitecbank.fraud.domain.RiskLevel;
import java.util.List;

public record EvaluationOutcome(
        int riskScore,
        RiskLevel riskLevel,
        Decision decision,
        String rulesVersion,
        List<RuleHit> hits) {
}
