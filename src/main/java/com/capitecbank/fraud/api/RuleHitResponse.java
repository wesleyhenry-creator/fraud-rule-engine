package com.capitecbank.fraud.api;

public record RuleHitResponse(int ruleOrder, String ruleCode, int weight, String reason) {
}
