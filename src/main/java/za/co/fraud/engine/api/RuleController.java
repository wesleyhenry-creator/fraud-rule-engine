package za.co.fraud.engine.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.fraud.engine.api.dto.RuleDescriptor;
import za.co.fraud.engine.scoring.FraudScoringEngine;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rules")
@Tag(name = "Rules", description = "Inspect the active fraud rule set")
public class RuleController {

    private final FraudScoringEngine engine;

    public RuleController(FraudScoringEngine engine) {
        this.engine = engine;
    }

    @GetMapping(produces = "application/json")
    @Operation(summary = "List the active fraud rules and their weights")
    public List<RuleDescriptor> rules() {
        return engine.rules().stream()
                .map(r -> new RuleDescriptor(r.code(), r.description(), r.weight()))
                .toList();
    }
}
