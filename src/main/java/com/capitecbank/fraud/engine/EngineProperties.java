package com.capitecbank.fraud.engine;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "fraud.engine")
public record EngineProperties(
        @DefaultValue("1.0.0") String rulesVersion,
        @DefaultValue("30") int referThreshold,
        @DefaultValue("60") int investigateThreshold,
        @DefaultValue("90") long historyLookbackDays) {

    public EngineProperties {
        if (referThreshold < 0 || investigateThreshold > 100 || referThreshold >= investigateThreshold) {
            throw new IllegalArgumentException(
                    "Thresholds must satisfy 0 <= referThreshold < investigateThreshold <= 100");
        }
    }
}
