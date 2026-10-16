package za.co.fraud.engine.rules;

import za.co.fraud.engine.domain.RuleOutcome;

import java.util.Optional;

/**
 * A single fraud indicator. Implementations must be stateless and side-effect free so that
 * they can be evaluated concurrently and tested in isolation.
 */
public interface FraudRule {

    /** Stable identifier persisted with each assessment, e.g. {@code HIGH_VALUE_CLAIM}. */
    String code();

    String description();

    /** Points added to the risk score when this rule fires. */
    int weight();

    /** @return the outcome if the rule fires for the context, otherwise empty */
    Optional<RuleOutcome> evaluate(RuleContext context);

    default RuleOutcome triggered(String reason) {
        return new RuleOutcome(code(), weight(), reason);
    }
}
