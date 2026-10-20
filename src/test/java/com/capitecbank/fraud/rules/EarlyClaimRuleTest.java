package com.capitecbank.fraud.rules;

import static com.capitecbank.fraud.support.TestEvents.event;
import static org.assertj.core.api.Assertions.assertThat;

import com.capitecbank.fraud.domain.TransactionCategory;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class EarlyClaimRuleTest {

    private static final Instant CLAIM_AT = Instant.parse("2026-10-20T08:00:00Z");
    private final EarlyClaimRule rule = new EarlyClaimRule(new EarlyClaimRuleProperties(true, 25, 30));

    @Test
    void firesWhenClaimIsWithinThresholdOfInception() {
        var claim = event(CLAIM_AT, TransactionCategory.CLAIM_SUBMISSION, "5000.00", LocalDate.of(2026, 10, 10));

        RuleResult result = rule.evaluate(new RuleContext(claim, List.of()));

        assertThat(result.fired()).isTrue();
        assertThat(result.reason()).contains("10 days after policy inception");
    }

    @Test
    void doesNotFireWhenInceptionDateIsUnknown() {
        var claim = event(CLAIM_AT, TransactionCategory.CLAIM_PAYOUT, "5000.00", null);

        assertThat(rule.evaluate(new RuleContext(claim, List.of())).fired()).isFalse();
    }

    @Test
    void doesNotFireWhenClaimIsOutsideThreshold() {
        var claim = event(CLAIM_AT, TransactionCategory.CLAIM_PAYOUT, "5000.00", LocalDate.of(2026, 9, 5));

        assertThat(rule.evaluate(new RuleContext(claim, List.of())).fired()).isFalse();
    }
}
