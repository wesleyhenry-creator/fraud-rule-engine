package za.co.fraud.engine.domain;

/** A fired rule: its code, the weight it contributed to the score, and a human-readable reason. */
public record RuleOutcome(String ruleCode, int weight, String reason) {
}
