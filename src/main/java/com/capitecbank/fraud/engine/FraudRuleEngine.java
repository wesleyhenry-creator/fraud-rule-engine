package com.capitecbank.fraud.engine;

import com.capitecbank.fraud.domain.Decision;
import com.capitecbank.fraud.domain.RiskLevel;
import com.capitecbank.fraud.domain.TransactionEvent;
import com.capitecbank.fraud.rules.AbstractFraudRule;
import com.capitecbank.fraud.rules.RuleContext;
import com.capitecbank.fraud.rules.RuleResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Runs every enabled rule that applies to a transaction's category, sums the weights of the
 * rules that fire (capped at 100), and maps the score to a risk level and decision.
 */
@Component
public class FraudRuleEngine {

    private static final int MAX_SCORE = 100;

    private final List<AbstractFraudRule> rules;
    private final EngineProperties properties;

    public FraudRuleEngine(List<AbstractFraudRule> rules, EngineProperties properties) {
        this.properties = properties;
        this.rules = rules.stream()
                .sorted(Comparator.comparingInt(AbstractFraudRule::order))
                .toList();
        validateUnique();
    }

    public List<AbstractFraudRule> rules() {
        return rules;
    }

    public EvaluationOutcome evaluate(TransactionEvent event, List<TransactionEvent> policyHistory) {
        RuleContext context = new RuleContext(event, policyHistory);
        List<RuleHit> hits = new ArrayList<>();

        for (AbstractFraudRule rule : rules) {
            if (!rule.appliesTo(event.getCategory())) {
                continue;
            }
            RuleResult result = rule.evaluate(context);
            if (result.fired()) {
                hits.add(new RuleHit(rule.order(), rule.code(), rule.weight(), result.reason()));
            }
        }

        int score = Math.min(MAX_SCORE, hits.stream().mapToInt(RuleHit::weight).sum());
        return new EvaluationOutcome(score, riskLevelFor(score), decisionFor(score),
                properties.rulesVersion(), List.copyOf(hits));
    }

    RiskLevel riskLevelFor(int score) {
        if (score >= properties.investigateThreshold()) {
            return RiskLevel.HIGH;
        }
        if (score >= properties.referThreshold()) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    Decision decisionFor(int score) {
        if (score >= properties.investigateThreshold()) {
            return Decision.FLAG_FOR_INVESTIGATION;
        }
        if (score >= properties.referThreshold()) {
            return Decision.REFER_FOR_REVIEW;
        }
        return Decision.CLEAR;
    }

    private void validateUnique() {
        Set<Integer> orders = new HashSet<>();
        Set<String> codes = new HashSet<>();
        for (AbstractFraudRule rule : rules) {
            if (!orders.add(rule.order())) {
                throw new IllegalStateException("Duplicate fraud rule order: " + rule.order());
            }
            if (!codes.add(rule.code())) {
                throw new IllegalStateException("Duplicate fraud rule code: " + rule.code());
            }
        }
    }
}
