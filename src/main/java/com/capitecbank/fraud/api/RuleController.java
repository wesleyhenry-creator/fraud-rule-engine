package com.capitecbank.fraud.api;

import com.capitecbank.fraud.service.RuleCatalogService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rules")
public class RuleController {

    private final RuleCatalogService catalog;

    public RuleController(RuleCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    public List<RuleDescriptorResponse> list() {
        return catalog.listRules();
    }
}
