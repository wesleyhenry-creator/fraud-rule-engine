package za.co.fraud.engine.scoring;

import org.springframework.stereotype.Component;
import za.co.fraud.engine.config.FraudProperties;
import za.co.fraud.engine.domain.Decision;
import za.co.fraud.engine.domain.RiskLevel;
import za.co.fraud.engine.domain.RuleOutcome;
import za.co.fraud.engine.rules.FraudRule;
import za.co.fraud.engine.rules.RuleContext;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Evaluates every rule against a transaction, sums the weights of the rules that fire
 * (capped at 100) and maps the score to a risk level and decision.
 */
@Component
public class FraudScoringEngine {

    static final int MAX_SCORE = 100;

    private final List<FraudRule> rules;
    private final FraudProperties props;

    public FraudScoringEngine(List<FraudRule> rules, FraudProperties props) {
        this.rules = rules.stream().sorted(Comparator.comparing(FraudRule::code)).toList();
        this.props = props;
    }

    public List<FraudRule> rules() {
        return rules;
    }

    public ScoringResult score(RuleContext context) {
        List<RuleOutcome> triggered = rules.stream()
                .map(rule -> rule.evaluate(context))
                .flatMap(Optional::stream)
                .toList();

        int rawScore = triggered.stream().mapToInt(RuleOutcome::weight).sum();
        int score = Math.min(MAX_SCORE, rawScore);

        RiskLevel level;
        Decision decision;
        if (score >= props.investigateThreshold()) {
            level = RiskLevel.HIGH;
            decision = Decision.FLAG_FOR_INVESTIGATION;
        } else if (score >= props.reviewThreshold()) {
            level = RiskLevel.MEDIUM;
            decision = Decision.REFER_FOR_REVIEW;
        } else {
            level = RiskLevel.LOW;
            decision = Decision.CLEAR;
        }
        return new ScoringResult(score, level, decision, triggered);
    }
}
