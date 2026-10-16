package za.co.fraud.engine.rules;

import org.springframework.stereotype.Component;
import za.co.fraud.engine.config.FraudProperties;
import za.co.fraud.engine.domain.RuleOutcome;
import za.co.fraud.engine.domain.TransactionEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Fires when a claim repeats an earlier claim by reference, or by amount and payee, within a short window. */
@Component
public class DuplicateClaimRule implements FraudRule {

    private final FraudProperties props;

    public DuplicateClaimRule(FraudProperties props) {
        this.props = props;
    }

    @Override
    public String code() {
        return "DUPLICATE_CLAIM";
    }

    @Override
    public String description() {
        return "Claim matches a recent claim on the same policy by reference, or by amount and payee";
    }

    @Override
    public int weight() {
        return props.duplicateClaimWeight();
    }

    @Override
    public Optional<RuleOutcome> evaluate(RuleContext ctx) {
        TransactionEvent tx = ctx.event();
        if (!tx.category().isClaim()) {
            return Optional.empty();
        }
        Instant windowStart = tx.occurredAt().minus(Duration.ofDays(props.duplicateWindowDays()));
        return ctx.policyHistory().stream()
                .filter(h -> h.category().isClaim())
                .filter(h -> !h.occurredAt().isBefore(windowStart))
                .filter(h -> sameReference(h, tx) || sameAmountAndPayee(h, tx))
                .findFirst()
                .map(prior -> triggered("Duplicate of claim %s within %d days (%s)".formatted(
                        prior.transactionId(),
                        props.duplicateWindowDays(),
                        sameReference(prior, tx) ? "same claim reference" : "same amount and payee")));
    }

    private static boolean sameReference(TransactionEvent prior, TransactionEvent tx) {
        return tx.claimReference() != null && tx.claimReference().equals(prior.claimReference());
    }

    private static boolean sameAmountAndPayee(TransactionEvent prior, TransactionEvent tx) {
        return prior.amount().compareTo(tx.amount()) == 0
                && prior.payeeAccountId().equals(tx.payeeAccountId());
    }
}
