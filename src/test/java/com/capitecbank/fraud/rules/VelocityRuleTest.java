package com.capitecbank.fraud.rules;

import static org.assertj.core.api.Assertions.assertThat;

import com.capitecbank.fraud.domain.TransactionCategory;
import com.capitecbank.fraud.domain.TransactionEvent;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static com.capitecbank.fraud.support.TestEvents.event;

class VelocityRuleTest {

    private static final Instant NOW = Instant.parse("2026-10-20T10:00:00Z");
    private final VelocityRule rule = new VelocityRule(new VelocityRuleProperties(true, 20, 60, 5));

    @Test
    void firesWhenWindowReachesThreshold() {
        List<TransactionEvent> history = List.of(
                priorMinutesBefore(40), priorMinutesBefore(30), priorMinutesBefore(20), priorMinutesBefore(10));

        RuleResult result = rule.evaluate(new RuleContext(current(), history));

        assertThat(result.fired()).isTrue();
        assertThat(result.reason()).contains("5 transactions");
    }

    @Test
    void doesNotFireBelowThreshold() {
        List<TransactionEvent> history = List.of(
                priorMinutesBefore(30), priorMinutesBefore(20), priorMinutesBefore(10));

        assertThat(rule.evaluate(new RuleContext(current(), history)).fired()).isFalse();
    }

    @Test
    void ignoresEventsOutsideWindow() {
        List<TransactionEvent> history = List.of(
                priorMinutesBefore(90), priorMinutesBefore(120),
                priorMinutesBefore(30), priorMinutesBefore(20), priorMinutesBefore(10));

        assertThat(rule.evaluate(new RuleContext(current(), history)).fired()).isFalse();
    }

    private TransactionEvent current() {
        return event(NOW, TransactionCategory.PREMIUM_PAYMENT, "1000.00");
    }

    private TransactionEvent priorMinutesBefore(long minutes) {
        return event(NOW.minusSeconds(minutes * 60), TransactionCategory.PREMIUM_PAYMENT, "1000.00");
    }
}
