package com.capitecbank.fraud.service;

import com.capitecbank.fraud.api.RuleDescriptorResponse;
import com.capitecbank.fraud.engine.FraudRuleEngine;
import com.capitecbank.fraud.rules.AbstractFraudRule;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RuleCatalogService {

    private final FraudRuleEngine engine;

    public RuleCatalogService(FraudRuleEngine engine) {
        this.engine = engine;
    }

    public List<RuleDescriptorResponse> listRules() {
        return engine.rules().stream()
                .map(AbstractFraudRule::describe)
                .map(d -> new RuleDescriptorResponse(
                        d.code(),
                        d.name(),
                        d.enabled(),
                        d.weight(),
                        d.order(),
                        d.categories().stream().sorted().toList(),
                        d.settings()))
                .toList();
    }
}
