package za.co.fraud.engine.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;
import za.co.fraud.engine.api.dto.AssessmentResponse;
import za.co.fraud.engine.api.dto.TransactionRequest;
import za.co.fraud.engine.domain.Assessment;
import za.co.fraud.engine.service.FraudAssessmentService;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Submit categorised transaction events for fraud assessment")
public class TransactionController {

    private final FraudAssessmentService service;

    public TransactionController(FraudAssessmentService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Assess a transaction against the fraud rules and store the result")
    public ResponseEntity<AssessmentResponse> submit(@Valid @RequestBody TransactionRequest request) {
        Assessment assessment = service.assess(request.toDomain());
        URI location = UriComponentsBuilder.fromPath("/api/v1/assessments/{id}")
                .buildAndExpand(assessment.transaction().transactionId())
                .encode()
                .toUri();
        return ResponseEntity.created(location).body(AssessmentResponse.from(assessment));
    }
}
