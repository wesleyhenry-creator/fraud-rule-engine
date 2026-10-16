package za.co.fraud.engine.rules;

import org.springframework.stereotype.Component;
import za.co.fraud.engine.config.FraudProperties;
import za.co.fraud.engine.domain.Masking;
import za.co.fraud.engine.domain.RuleOutcome;
import za.co.fraud.engine.domain.TransactionCategory;
import za.co.fraud.engine.domain.TransactionEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;

/**
 * Fires when a payout is sent to an account that was set as the policy beneficiary shortly before.
 * This is the typical account-takeover pattern: redirect the beneficiary, then claim.
 */
@Component
public class RecentBeneficiaryChangePayoutRule implements FraudRule {

    private final FraudProperties props;

    public RecentBeneficiaryChangePayoutRule(FraudProperties props) {
        this.props = props;
    }

    @Override
    public String code() {
        return "RECENT_BENEFICIARY_CHANGE_PAYOUT";
    }

    @Override
    public String description() {
        return "Payout to an account set as beneficiary within the configured window";
    }

    @Override
    public int weight() {
        return props.beneficiaryChangeWeight();
    }

    @Override
    public Optional<RuleOutcome> evaluate(RuleContext ctx) {
        TransactionEvent tx = ctx.event();
        if (tx.category() != TransactionCategory.CLAIM_PAYOUT) {
            return Optional.empty();
        }
        Instant windowStart = tx.occurredAt().minus(Duration.ofDays(props.beneficiaryChangeWindowDays()));
        return ctx.policyHistory().stream()
                .filter(h -> h.category() == TransactionCategory.BENEFICIARY_UPDATE)
                .filter(h -> !h.occurredAt().isBefore(windowStart))
                .filter(h -> tx.payeeAccountId().equals(h.payeeAccountId()))
                .max(Comparator.comparing(TransactionEvent::occurredAt))
                .map(update -> triggered("Payout to account %s, set as beneficiary %d day(s) earlier".formatted(
                        Masking.account(tx.payeeAccountId()),
                        Duration.between(update.occurredAt(), tx.occurredAt()).toDays())));
    }
}
