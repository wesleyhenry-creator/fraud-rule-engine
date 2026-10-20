package com.capitecbank.fraud.rules;

import com.capitecbank.fraud.domain.TransactionCategory;
import java.util.Map;
import java.util.Set;

public record RuleDescriptor(
        String code,
        String name,
        boolean enabled,
        int weight,
        int order,
        Set<TransactionCategory> categories,
        Map<String, Object> settings) {
}
