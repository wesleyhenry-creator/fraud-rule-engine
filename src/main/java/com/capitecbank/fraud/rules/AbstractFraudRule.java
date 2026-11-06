package com.capitecbank.fraud.rules;

import com.capitecbank.fraud.domain.TransactionCategory;
import java.util.Map;
import java.util.Set;

/**
 * Base type for every fraud rule. Concrete rules are Spring components, so the engine
 * discovers them automatically and they appear in GET /api/v1/rules.
 */
public abstract class AbstractFraudRule {

    /** Stable identifier stored in fraud_assessment_rule.rule_code. Do not rename once released. */
    public abstract String code();

    public abstract String name();

    /** Transaction categories this rule is evaluated for. */
    public abstract Set<TransactionCategory> categories();

    /** Evaluation order. Must be unique across rules; stored as fraud_assessment_rule.rule_order. */
    public abstract int order();

    public abstract boolean enabled();

    /** Points added to the risk score when the rule fires. */
    public abstract int weight();

    public abstract RuleResult evaluate(RuleContext context);

    /** Effective settings, exposed for transparency through the rules API. */
    protected abstract Map<String, Object> settings();

    public final boolean appliesTo(TransactionCategory category) {
        return enabled() && categories().contains(category);
    }

    public final RuleDescriptor describe() {
        return new RuleDescriptor(code(), name(), enabled(), weight(), order(), categories(), settings());
    }
}
