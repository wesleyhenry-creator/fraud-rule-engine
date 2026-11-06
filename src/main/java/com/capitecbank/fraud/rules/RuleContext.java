package com.capitecbank.fraud.rules;

import com.capitecbank.fraud.domain.TransactionEvent;
import java.util.List;
import java.util.Objects;

/**
 * Input to a rule.
 *
 * @param event         the transaction being assessed
 * @param policyHistory earlier events on the same policy inside the engine lookback, oldest first;
 *                      never contains the current event
 */
public record RuleContext(TransactionEvent event, List<TransactionEvent> policyHistory) {

    public RuleContext {
        Objects.requireNonNull(event, "event");
        policyHistory = policyHistory == null ? List.of() : List.copyOf(policyHistory);
    }
}
