package com.capitecbank.fraud.engine;

import static com.capitecbank.fraud.support.TestEvents.event;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.capitecbank.fraud.domain.Decision;
import com.capitecbank.fraud.domain.RiskLevel;
import com.capitecbank.fraud.domain.TransactionCategory;
import com.capitecbank.fraud.domain.TransactionEvent;
import com.capitecbank.fraud.rules.EarlyClaimRule;
import com.capitecbank.fraud.rules.EarlyClaimRuleProperties;
import com.capitecbank.fraud.rules.HighAmountRule;
import com.capitecbank.fraud.rules.HighAmountRuleProperties;
import com.capitecbank.fraud.rules.VelocityRule;
import com.capitecbank.fraud.rules.VelocityRuleProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class FraudRuleEngineTest {

    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");

    private final VelocityRule velocity = new VelocityRule(new VelocityRuleProperties(true, 20, 60, 5));
    private final EarlyClaimRule earlyClaim = new EarlyClaimRule(new EarlyClaimRuleProperties(true, 25, 30));
    private final HighAmountRule highAmount = new HighAmountRule(
            new HighAmountRuleProperties(true, 15, new BigDecimal("50000.00"), new BigDecimal("10")));

    private final FraudRuleEngine engine = new FraudRuleEngine(
            List.of(velocity, earlyClaim, highAmount), new EngineProperties("1.0.0", 30, 60, 90));

    @Test
    void sumsFiredRulesIntoReferDecision() {
        List<TransactionEvent> history = List.of(
                event(NOW.minusSeconds(600), TransactionCategory.PREMIUM_PAYMENT, "1000.00"),
                event(NOW.minusSeconds(1200), TransactionCategory.PREMIUM_PAYMENT, "1000.00"),
                event(NOW.minusSeconds(1800), TransactionCategory.PREMIUM_PAYMENT, "1000.00"),
                event(NOW.minusSeconds(2400), TransactionCategory.PREMIUM_PAYMENT, "1000.00"));
        var claim = event(NOW, TransactionCategory.CLAIM_PAYOUT, "1000.00", LocalDate.of(2026, 10, 10));

        EvaluationOutcome outcome = engine.evaluate(claim, history);

        assertThat(outcome.riskScore()).isEqualTo(45);
        assertThat(outcome.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(outcome.decision()).isEqualTo(Decision.REFER_FOR_REVIEW);
        assertThat(outcome.hits()).extracting(RuleHit::ruleCode).containsExactly("VELOCITY", "EARLY_CLAIM");
    }

    @Test
    void clearsTransactionWhenNoRuleFires() {
        var premium = event(NOW, TransactionCategory.PREMIUM_PAYMENT, "1000.00");

        EvaluationOutcome outcome = engine.evaluate(premium, List.of());

        assertThat(outcome.riskScore()).isZero();
        assertThat(outcome.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(outcome.decision()).isEqualTo(Decision.CLEAR);
        assertThat(outcome.hits()).isEmpty();
    }

    @Test
    void rejectsRulesWithDuplicateOrder() {
        assertThatThrownBy(() -> new FraudRuleEngine(
                List.of(velocity, new VelocityRule(new VelocityRuleProperties(true, 5, 60, 5))),
                new EngineProperties("1.0.0", 30, 60, 90)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate fraud rule order");
    }
}
