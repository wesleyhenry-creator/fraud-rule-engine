package com.capitecbank.fraud.rules;

import com.capitecbank.fraud.domain.TransactionCategory;
import com.capitecbank.fraud.domain.TransactionEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Fires when the policy reaches too many transactions inside a sliding time window. */
@Component
public class VelocityRule extends AbstractFraudRule {

    public static final String CODE = "VELOCITY";
    private static final int ORDER = 10;

    private final VelocityRuleProperties properties;

    public VelocityRule(VelocityRuleProperties properties) {
        this.properties = properties;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String name() {
        return "Policy transaction velocity";
    }

    @Override
    public Set<TransactionCategory> categories() {
        return EnumSet.allOf(TransactionCategory.class);
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public boolean enabled() {
        return properties.enabled();
    }

    @Override
    public int weight() {
        return properties.weight();
    }

    @Override
    public RuleResult evaluate(RuleContext context) {
        Instant occurredAt = context.event().getOccurredAt();
        Instant windowStart = occurredAt.minus(Duration.ofMinutes(properties.windowMinutes()));

        long priorInWindow = context.policyHistory().stream()
                .filter(e -> !e.getOccurredAt().isBefore(windowStart) && e.getOccurredAt().isBefore(occurredAt))
                .count();
        long total = priorInWindow + 1; // include the current event

        if (total < properties.maxTransactions()) {
            return RuleResult.notFired();
        }
        return RuleResult.fired(String.format("Policy has %d transactions within %d minutes (threshold %d)",
                total, properties.windowMinutes(), properties.maxTransactions()));
    }

    @Override
    protected Map<String, Object> settings() {
        return Map.<String, Object>of(
                "windowMinutes", properties.windowMinutes(),
                "maxTransactions", properties.maxTransactions());
    }
}
