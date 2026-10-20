package com.capitecbank.fraud.rules;

import com.capitecbank.fraud.domain.TransactionCategory;
import com.capitecbank.fraud.domain.TransactionEvent;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Fires when a claim amount is at least the minimum amount and exceeds the configured
 * multiple of premiums paid on the policy within the history lookback.
 */
@Component
public class HighAmountRule extends AbstractFraudRule {

    public static final String CODE = "HIGH_AMOUNT";
    private static final int ORDER = 30;

    private final HighAmountRuleProperties properties;

    public HighAmountRule(HighAmountRuleProperties properties) {
        this.properties = properties;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String name() {
        return "High claim amount relative to premiums paid";
    }

    @Override
    public Set<TransactionCategory> categories() {
        return EnumSet.of(TransactionCategory.CLAIM_SUBMISSION, TransactionCategory.CLAIM_PAYOUT);
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
        BigDecimal amount = context.event().getAmount();
        if (amount.compareTo(properties.minimumAmount()) < 0) {
            return RuleResult.notFired();
        }
        BigDecimal premiumsPaid = context.policyHistory().stream()
                .filter(e -> e.getCategory() == TransactionCategory.PREMIUM_PAYMENT)
                .map(TransactionEvent::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal allowed = premiumsPaid.multiply(properties.premiumMultiple());
        if (amount.compareTo(allowed) <= 0) {
            return RuleResult.notFired();
        }
        return RuleResult.fired(String.format("Claim amount R%s exceeds %sx premiums paid (R%s)",
                amount.toPlainString(),
                properties.premiumMultiple().stripTrailingZeros().toPlainString(),
                premiumsPaid.toPlainString()));
    }

    @Override
    protected Map<String, Object> settings() {
        return Map.<String, Object>of(
                "minimumAmount", properties.minimumAmount(),
                "premiumMultiple", properties.premiumMultiple());
    }
}
