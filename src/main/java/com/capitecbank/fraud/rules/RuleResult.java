package com.capitecbank.fraud.rules;

public record RuleResult(boolean fired, String reason) {

    public static RuleResult notFired() {
        return new RuleResult(false, null);
    }

    public static RuleResult fired(String reason) {
        return new RuleResult(true, reason);
    }
}
