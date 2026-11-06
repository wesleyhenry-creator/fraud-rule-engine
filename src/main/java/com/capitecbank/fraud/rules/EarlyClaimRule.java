package com.capitecbank.fraud.rules;

import com.capitecbank.fraud.domain.TransactionCategory;
import com.capitecbank.fraud.domain.TransactionEvent;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Fires when a claim is raised shortly after the policy incepted. */
@Component
public class EarlyClaimRule extends AbstractFraudRule {

    public static final String CODE = "EARLY_CLAIM";
    private static final int ORDER = 20;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Africa/Johannesburg");

    private final EarlyClaimRuleProperties properties;

    public EarlyClaimRule(EarlyClaimRuleProperties properties) {
        this.properties = properties;
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String name() {
        return "Early claim after policy inception";
    }

    @Override
    public Set<TransactionCategory> categories() {
        return EnumSet.of(TransactionCategory.CLAIM_SUBMISSION, TransactionCategory.CLAIM_PAYOUT);
    }

    @Override
    public int order() {
        return ORDER;
    }

    @Override
    public boolean enabled() {
        return properties.enabled();
    }

    @Override
    public int weight() {
        return properties.weight();
    }

    @Override
    public RuleResult evaluate(RuleContext context) {
        TransactionEvent event = context.event();
        LocalDate inception = event.getPolicyInceptionDate();
        if (inception == null) {
            return RuleResult.notFired();
        }
        LocalDate eventDate = event.getOccurredAt().atZone(BUSINESS_ZONE).toLocalDate();
        long days = ChronoUnit.DAYS.between(inception, eventDate);
        if (days < 0 || days > properties.maxDaysSinceInception()) {
            return RuleResult.notFired();
        }
        return RuleResult.fired(String.format("Claim raised %d days after policy inception (threshold %d days)",
                days, properties.maxDaysSinceInception()));
    }

    @Override
    protected Map<String, Object> settings() {
        return Map.<String, Object>of("maxDaysSinceInception", properties.maxDaysSinceInception());
    }
}
