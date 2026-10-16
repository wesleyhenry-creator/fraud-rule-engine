package za.co.fraud.engine.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.fraud.engine.api.dto.AssessmentResponse;
import za.co.fraud.engine.api.dto.PageResponse;
import za.co.fraud.engine.domain.Decision;
import za.co.fraud.engine.domain.RiskLevel;
import za.co.fraud.engine.service.FraudAssessmentService;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/assessments")
@Validated
@Tag(name = "Assessments", description = "Retrieve stored fraud assessments")
public class AssessmentController {

    private final FraudAssessmentService service;

    public AssessmentController(FraudAssessmentService service) {
        this.service = service;
    }

    @GetMapping(value = "/{transactionId}", produces = "application/json")
    @Operation(summary = "Get the assessment for a transaction")
    public AssessmentResponse getByTransactionId(
            @PathVariable @Size(min = 1, max = 64) String transactionId) {
        return AssessmentResponse.from(service.getByTransactionId(transactionId));
    }

    @GetMapping(produces = "application/json")
    @Operation(summary = "Search assessments, newest first")
    public PageResponse<AssessmentResponse> search(
            @RequestParam(required = false) Decision decision,
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(required = false) @Size(max = 40) String policyNumber,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "evaluatedAt"));
        return PageResponse.from(
                service.search(decision, riskLevel, policyNumber, from, to, pageable)
                        .map(AssessmentResponse::from));
    }
}
