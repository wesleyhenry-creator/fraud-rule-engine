package za.co.fraud.engine.rules;

import za.co.fraud.engine.domain.TransactionEvent;

import java.time.Instant;
import java.util.List;

/**
 * Input to every rule.
 *
 * @param event         the transaction being assessed
 * @param policyHistory prior transactions on the same policy, oldest first, all occurring before {@code event}
 * @param evaluatedAt   wall-clock evaluation time (kept separate from event time so rules stay deterministic)
 */
public record RuleContext(TransactionEvent event, List<TransactionEvent> policyHistory, Instant evaluatedAt) {

    public RuleContext {
        policyHistory = List.copyOf(policyHistory);
    }
}
