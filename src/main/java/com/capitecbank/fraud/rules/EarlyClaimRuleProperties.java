package com.capitecbank.fraud.rules;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "fraud.rules.early-claim")
public record EarlyClaimRuleProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("25") int weight,
        @DefaultValue("30") long maxDaysSinceInception) {
}
