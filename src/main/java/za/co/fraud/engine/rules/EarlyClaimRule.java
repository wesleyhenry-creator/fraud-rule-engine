package za.co.fraud.engine.rules;

import org.springframework.stereotype.Component;
import za.co.fraud.engine.config.FraudProperties;
import za.co.fraud.engine.domain.RuleOutcome;
import za.co.fraud.engine.domain.TransactionEvent;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/** Fires when a claim is lodged shortly after the policy incepted, a classic pre-loss-fraud indicator. */
@Component
public class EarlyClaimRule implements FraudRule {

    /** Business dates are evaluated in South African time. */
    static final ZoneId BUSINESS_ZONE = ZoneId.of("Africa/Johannesburg");

    private final FraudProperties props;

    public EarlyClaimRule(FraudProperties props) {
        this.props = props;
    }

    @Override
    public String code() {
        return "EARLY_CLAIM";
    }

    @Override
    public String description() {
        return "Claim lodged within the configured window after policy inception";
    }

    @Override
    public int weight() {
        return props.earlyClaimWeight();
    }

    @Override
    public Optional<RuleOutcome> evaluate(RuleContext ctx) {
        TransactionEvent tx = ctx.event();
        if (!tx.category().isClaim() || tx.policyInceptionDate() == null) {
            return Optional.empty();
        }
        long days = ChronoUnit.DAYS.between(
                tx.policyInceptionDate(), tx.occurredAt().atZone(BUSINESS_ZONE).toLocalDate());
        if (days < 0 || days > props.earlyClaimWindowDays()) {
            return Optional.empty();
        }
        return Optional.of(triggered("Claim lodged %d day(s) after policy inception (window %d days)".formatted(
                days, props.earlyClaimWindowDays())));
    }
}
