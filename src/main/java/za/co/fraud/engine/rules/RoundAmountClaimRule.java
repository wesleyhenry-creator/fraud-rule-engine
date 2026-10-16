package za.co.fraud.engine.rules;

import org.springframework.stereotype.Component;
import za.co.fraud.engine.config.FraudProperties;
import za.co.fraud.engine.domain.RuleOutcome;
import za.co.fraud.engine.domain.TransactionEvent;

import java.math.BigDecimal;
import java.util.Optional;

/** Fires on large claims that are exact multiples of a round figure, a common trait of fabricated claims. */
@Component
public class RoundAmountClaimRule implements FraudRule {

    private final FraudProperties props;

    public RoundAmountClaimRule(FraudProperties props) {
        this.props = props;
    }

    @Override
    public String code() {
        return "ROUND_AMOUNT_CLAIM";
    }

    @Override
    public String description() {
        return "Claim is a round amount (exact multiple of the configured figure) above the minimum";
    }

    @Override
    public int weight() {
        return props.roundAmountWeight();
    }

    @Override
    public Optional<RuleOutcome> evaluate(RuleContext ctx) {
        TransactionEvent tx = ctx.event();
        BigDecimal amount = tx.amount();
        if (!tx.category().isClaim()
                || amount.compareTo(props.roundAmountMinimum()) < 0
                || amount.remainder(props.roundAmountMultiple()).signum() != 0) {
            return Optional.empty();
        }
        return Optional.of(triggered("Claim of R%s is a round amount (multiple of R%s)".formatted(
                amount.toPlainString(), props.roundAmountMultiple().toPlainString())));
    }
}
