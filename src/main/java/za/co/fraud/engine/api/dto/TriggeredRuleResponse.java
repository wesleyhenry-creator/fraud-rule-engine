package za.co.fraud.engine.api.dto;

public record TriggeredRuleResponse(String ruleCode, int weight, String reason) {
}
