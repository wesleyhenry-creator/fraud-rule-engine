package za.co.fraud.engine.rules;

import org.springframework.stereotype.Component;
import za.co.fraud.engine.config.FraudProperties;
import za.co.fraud.engine.domain.RuleOutcome;
import za.co.fraud.engine.domain.TransactionEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Fires when a policy lodges more claims than allowed within a rolling window (claim farming). */
@Component
public class ClaimVelocityRule implements FraudRule {

    private final FraudProperties props;

    public ClaimVelocityRule(FraudProperties props) {
        this.props = props;
    }

    @Override
    public String code() {
        return "CLAIM_VELOCITY";
    }

    @Override
    public String description() {
        return "More claims on one policy than permitted within the rolling window";
    }

    @Override
    public int weight() {
        return props.claimVelocityWeight();
    }

    @Override
    public Optional<RuleOutcome> evaluate(RuleContext ctx) {
        TransactionEvent tx = ctx.event();
        if (!tx.category().isClaim()) {
            return Optional.empty();
        }
        Instant windowStart = tx.occurredAt().minus(Duration.ofDays(props.claimVelocityWindowDays()));
        long priorClaims = ctx.policyHistory().stream()
                .filter(h -> h.category().isClaim())
                .filter(h -> !h.occurredAt().isBefore(windowStart))
                .count();

        // priorClaims + the current claim must not exceed the configured maximum
        if (priorClaims < props.claimVelocityMaxClaims()) {
            return Optional.empty();
        }
        return Optional.of(triggered("%d claims on this policy within %d days exceeds the limit of %d".formatted(
                priorClaims + 1, props.claimVelocityWindowDays(), props.claimVelocityMaxClaims())));
    }
}
