package com.capitecbank.fraud.api;

import com.capitecbank.fraud.domain.TransactionCategory;
import java.util.List;
import java.util.Map;

public record RuleDescriptorResponse(
        String code,
        String name,
        boolean enabled,
        int weight,
        int order,
        List<TransactionCategory> categories,
        Map<String, Object> settings) {
}
