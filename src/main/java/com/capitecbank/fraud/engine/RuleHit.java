package com.capitecbank.fraud.engine;

public record RuleHit(int ruleOrder, String ruleCode, int weight, String reason) {
}
