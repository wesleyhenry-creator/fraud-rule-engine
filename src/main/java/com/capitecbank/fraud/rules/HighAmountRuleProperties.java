package com.capitecbank.fraud.rules;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "fraud.rules.high-amount")
public record HighAmountRuleProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("15") int weight,
        @DefaultValue("50000.00") BigDecimal minimumAmount,
        @DefaultValue("10") BigDecimal premiumMultiple) {
}
