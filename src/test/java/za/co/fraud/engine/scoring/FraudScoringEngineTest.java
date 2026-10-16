package za.co.fraud.engine.scoring;

import org.junit.jupiter.api.Test;
import za.co.fraud.engine.config.FraudProperties;
import za.co.fraud.engine.domain.Decision;
import za.co.fraud.engine.domain.RiskLevel;
import za.co.fraud.engine.domain.RuleOutcome;
import za.co.fraud.engine.domain.TransactionCategory;
import za.co.fraud.engine.domain.TransactionEvent;
import za.co.fraud.engine.rules.FraudRule;
import za.co.fraud.engine.rules.RuleContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FraudScoringEngineTest {

    private static final RuleContext CONTEXT = new RuleContext(
            new TransactionEvent("t1", "POL-1", "ACC-1", "ACC-1", TransactionCategory.CLAIM_PAYOUT,
                    new BigDecimal("1.00"), Instant.parse("2026-10-10T10:00:00Z"), null, null),
            List.of(),
            Instant.parse("2026-10-12T08:00:00Z"));

    private static FraudRule stub(String code, int weight) {
        return new FraudRule() {
            @Override
            public String code() {
                return code;
            }

            @Override
            public String description() {
                return "stub";
            }

            @Override
            public int weight() {
                return weight;
            }

            @Override
            public Optional<RuleOutcome> evaluate(RuleContext context) {
                return Optional.of(triggered("stub " + code));
            }
        };
    }

    private static FraudScoringEngine engine(FraudRule... rules) {
        return new FraudScoringEngine(List.of(rules), FraudProperties.defaults());
    }

    @Test
    void scoreAtOrAboveInvestigateThresholdIsFlagged() {
        var result = engine(stub("A", 60), stub("B", 30)).score(CONTEXT);
        assertThat(result.riskScore()).isEqualTo(90);
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.decision()).isEqualTo(Decision.FLAG_FOR_INVESTIGATION);
        assertThat(result.triggeredRules()).hasSize(2);
    }

    @Test
    void scoreBetweenThresholdsIsReferred() {
        var result = engine(stub("A", 25), stub("B", 20)).score(CONTEXT);
        assertThat(result.riskScore()).isEqualTo(45);
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(result.decision()).isEqualTo(Decision.REFER_FOR_REVIEW);
    }

    @Test
    void scoreBelowReviewThresholdIsCleared() {
        var result = engine(stub("A", 10)).score(CONTEXT);
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(result.decision()).isEqualTo(Decision.CLEAR);
    }

    @Test
    void scoreIsCappedAtOneHundred() {
        var result = engine(stub("A", 70), stub("B", 50)).score(CONTEXT);
        assertThat(result.riskScore()).isEqualTo(100);
    }

    @Test
    void noRulesFiredIsCleared() {
        var result = engine().score(CONTEXT);
        assertThat(result.riskScore()).isZero();
        assertThat(result.decision()).isEqualTo(Decision.CLEAR);
    }

    @Test
    void rulesAreExposedInDeterministicOrder() {
        var e = engine(stub("ZETA", 1), stub("ALPHA", 1));
        assertThat(e.rules()).extracting(FraudRule::code).containsExactly("ALPHA", "ZETA");
    }
}
