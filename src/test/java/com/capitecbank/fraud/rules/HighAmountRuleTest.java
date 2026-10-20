package com.capitecbank.fraud.rules;

import static com.capitecbank.fraud.support.TestEvents.event;
import static org.assertj.core.api.Assertions.assertThat;

import com.capitecbank.fraud.domain.TransactionCategory;
import com.capitecbank.fraud.domain.TransactionEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class HighAmountRuleTest {

    private static final Instant CLAIM_AT = Instant.parse("2026-10-20T08:00:00Z");
    private final HighAmountRule rule = new HighAmountRule(
            new HighAmountRuleProperties(true, 15, new BigDecimal("50000.00"), new BigDecimal("10")));

    @Test
    void firesWhenLargeClaimHasNoPremiumsOnRecord() {
        var claim = event(CLAIM_AT, TransactionCategory.CLAIM_PAYOUT, "60000.00");

        assertThat(rule.evaluate(new RuleContext(claim, List.of())).fired()).isTrue();
    }

    @Test
    void doesNotFireWhenAmountIsWithinPremiumMultiple() {
        TransactionEvent premium = event(CLAIM_AT.minusSeconds(86_400), TransactionCategory.PREMIUM_PAYMENT, "10000.00");
        var claim = event(CLAIM_AT, TransactionCategory.CLAIM_PAYOUT, "60000.00");

        // 10 x 10000 = 100000 allowed, so 60000 is not flagged
        assertThat(rule.evaluate(new RuleContext(claim, List.of(premium))).fired()).isFalse();
    }

    @Test
    void doesNotFireBelowMinimumAmount() {
        var claim = event(CLAIM_AT, TransactionCategory.CLAIM_SUBMISSION, "10000.00");

        assertThat(rule.evaluate(new RuleContext(claim, List.of())).fired()).isFalse();
    }
}
