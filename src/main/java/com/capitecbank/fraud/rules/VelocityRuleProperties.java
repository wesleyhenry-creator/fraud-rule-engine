package com.capitecbank.fraud.rules;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "fraud.rules.velocity")
public record VelocityRuleProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("20") int weight,
        @DefaultValue("60") long windowMinutes,
        @DefaultValue("5") int maxTransactions) {
}
