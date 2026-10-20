package com.capitecbank.fraud.api;

import com.capitecbank.fraud.domain.Decision;
import com.capitecbank.fraud.domain.RiskLevel;
import com.capitecbank.fraud.service.FraudAssessmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assessments")
public class AssessmentController {

    private static final int MAX_PAGE_SIZE = 100;

    private final FraudAssessmentService service;

    public AssessmentController(FraudAssessmentService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<AssessmentResponse> list(
            @RequestParam(required = false) Decision decision,
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return service.list(decision, riskLevel, safePage, safeSize);
    }
}
