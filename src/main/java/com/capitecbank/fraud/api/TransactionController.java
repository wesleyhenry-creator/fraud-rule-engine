package com.capitecbank.fraud.api;

import com.capitecbank.fraud.service.FraudAssessmentService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final FraudAssessmentService service;

    public TransactionController(FraudAssessmentService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AssessmentResponse> assess(@Valid @RequestBody TransactionEventRequest request) {
        AssessmentResponse response = service.assess(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{transactionId}/assessment")
                .buildAndExpand(request.transactionId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{transactionId}/assessment")
    public AssessmentResponse getAssessment(@PathVariable String transactionId) {
        return service.getByTransactionId(transactionId);
    }
}
