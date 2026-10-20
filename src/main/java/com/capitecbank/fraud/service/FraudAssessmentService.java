package com.capitecbank.fraud.service;

import com.capitecbank.fraud.api.AssessmentResponse;
import com.capitecbank.fraud.api.PageResponse;
import com.capitecbank.fraud.api.RuleHitResponse;
import com.capitecbank.fraud.api.TransactionEventRequest;
import com.capitecbank.fraud.common.Masking;
import com.capitecbank.fraud.domain.Decision;
import com.capitecbank.fraud.domain.FraudAssessment;
import com.capitecbank.fraud.domain.RiskLevel;
import com.capitecbank.fraud.domain.TransactionEvent;
import com.capitecbank.fraud.engine.EngineProperties;
import com.capitecbank.fraud.engine.EvaluationOutcome;
import com.capitecbank.fraud.engine.FraudRuleEngine;
import com.capitecbank.fraud.engine.RuleHit;
import com.capitecbank.fraud.error.AssessmentNotFoundException;
import com.capitecbank.fraud.error.DuplicateTransactionException;
import com.capitecbank.fraud.persistence.FraudAssessmentRepository;
import com.capitecbank.fraud.persistence.TransactionEventRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudAssessmentService {

    private final TransactionEventRepository transactionEvents;
    private final FraudAssessmentRepository assessments;
    private final FraudRuleEngine engine;
    private final EngineProperties engineProperties;
    private final Clock clock;

    public FraudAssessmentService(TransactionEventRepository transactionEvents,
                                  FraudAssessmentRepository assessments,
                                  FraudRuleEngine engine,
                                  EngineProperties engineProperties,
                                  Clock clock) {
        this.transactionEvents = transactionEvents;
        this.assessments = assessments;
        this.engine = engine;
        this.engineProperties = engineProperties;
        this.clock = clock;
    }

    @Transactional
    public AssessmentResponse assess(TransactionEventRequest request) {
        if (transactionEvents.existsByTransactionId(request.transactionId())) {
            throw new DuplicateTransactionException(request.transactionId());
        }

        Instant now = clock.instant();
        TransactionEvent event = transactionEvents.save(new TransactionEvent(
                request.transactionId(),
                request.policyNumber(),
                request.accountId(),
                request.payeeAccountId(),
                request.category(),
                request.amount(),
                request.occurredAt(),
                request.claimReference(),
                request.policyInceptionDate(),
                now));

        Instant historyFrom = event.getOccurredAt().minus(Duration.ofDays(engineProperties.historyLookbackDays()));
        List<TransactionEvent> history = transactionEvents
                .findByPolicyNumberAndOccurredAtGreaterThanEqualAndOccurredAtBeforeOrderByOccurredAtAsc(
                        event.getPolicyNumber(), historyFrom, event.getOccurredAt());

        EvaluationOutcome outcome = engine.evaluate(event, history);

        FraudAssessment assessment = new FraudAssessment(event, outcome.riskScore(), outcome.riskLevel(),
                outcome.decision(), outcome.rulesVersion(), now);
        for (RuleHit hit : outcome.hits()) {
            assessment.addRule(hit.ruleOrder(), hit.ruleCode(), hit.weight(), hit.reason());
        }

        FraudAssessment saved = assessments.save(assessment);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public AssessmentResponse getByTransactionId(String transactionId) {
        return assessments.findByTransactionEventTransactionId(transactionId)
                .map(this::toResponse)
                .orElseThrow(() -> new AssessmentNotFoundException(transactionId));
    }

    @Transactional(readOnly = true)
    public PageResponse<AssessmentResponse> list(Decision decision, RiskLevel riskLevel, int page, int size) {
        Page<FraudAssessment> result = assessments.search(decision, riskLevel, PageRequest.of(page, size));
        return new PageResponse<>(
                result.map(this::toResponse).getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private AssessmentResponse toResponse(FraudAssessment assessment) {
        TransactionEvent event = assessment.getTransactionEvent();
        List<RuleHitResponse> rules = assessment.getRules().stream()
                .map(r -> new RuleHitResponse(r.getId().getRuleOrder(), r.getRuleCode(), r.getWeight(), r.getReason()))
                .toList();
        return new AssessmentResponse(
                event.getTransactionId(),
                event.getPolicyNumber(),
                Masking.mask(event.getAccountId()),
                Masking.mask(event.getPayeeAccountId()),
                event.getCategory(),
                event.getAmount(),
                event.getOccurredAt(),
                assessment.getId(),
                assessment.getRiskScore(),
                assessment.getRiskLevel(),
                assessment.getDecision(),
                assessment.getRulesVersion(),
                assessment.getEvaluatedAt(),
                rules);
    }
}
