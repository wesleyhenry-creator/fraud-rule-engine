package za.co.fraud.engine.rules;

import org.springframework.stereotype.Component;
import za.co.fraud.engine.config.FraudProperties;
import za.co.fraud.engine.domain.RuleOutcome;
import za.co.fraud.engine.domain.TransactionEvent;

import java.util.Optional;

/** Fires when a claim is for a large amount, the most direct indicator of insurance payout fraud. */
@Component
public class HighValueClaimRule implements FraudRule {

    private final FraudProperties props;

    public HighValueClaimRule(FraudProperties props) {
        this.props = props;
    }

    @Override
    public String code() {
        return "HIGH_VALUE_CLAIM";
    }

    @Override
    public String description() {
        return "Claim amount at or above the configured high-value threshold";
    }

    @Override
    public int weight() {
        return props.highValueClaimWeight();
    }

    @Override
    public Optional<RuleOutcome> evaluate(RuleContext ctx) {
        TransactionEvent tx = ctx.event();
        if (!tx.category().isClaim() || tx.amount().compareTo(props.highValueClaimThreshold()) < 0) {
            return Optional.empty();
        }
        return Optional.of(triggered("Claim of R%s is at or above the high-value threshold of R%s".formatted(
                tx.amount().toPlainString(), props.highValueClaimThreshold().toPlainString())));
    }
}
