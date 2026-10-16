package za.co.fraud.engine.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.fraud.engine.config.FraudProperties;
import za.co.fraud.engine.domain.Assessment;
import za.co.fraud.engine.domain.Decision;
import za.co.fraud.engine.domain.RiskLevel;
import za.co.fraud.engine.domain.TransactionEvent;
import za.co.fraud.engine.persistence.AssessmentSpecifications;
import za.co.fraud.engine.persistence.FraudAssessmentEntity;
import za.co.fraud.engine.persistence.FraudAssessmentRepository;
import za.co.fraud.engine.persistence.TransactionEventEntity;
import za.co.fraud.engine.persistence.TransactionEventRepository;
import za.co.fraud.engine.rules.RuleContext;
import za.co.fraud.engine.scoring.FraudScoringEngine;
import za.co.fraud.engine.scoring.ScoringResult;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class FraudAssessmentService {

    private static final Logger log = LoggerFactory.getLogger(FraudAssessmentService.class);

    /** Bump when rule logic or weights change so stored results can be traced to the rule set that produced them. */
    public static final String RULES_VERSION = "2026.10.1";

    private final TransactionEventRepository events;
    private final FraudAssessmentRepository assessments;
    private final FraudScoringEngine engine;
    private final Clock clock;
    private final MeterRegistry meters;

    public FraudAssessmentService(TransactionEventRepository events,
                                  FraudAssessmentRepository assessments,
                                  FraudScoringEngine engine,
                                  Clock clock,
                                  MeterRegistry meters) {
        this.events = events;
        this.assessments = assessments;
        this.engine = engine;
        this.clock = clock;
        this.meters = meters;
    }

    /**
     * Evaluates a transaction against the rule set and persists the event and its assessment atomically.
     *
     * @throws DuplicateTransactionException if the transaction id was already assessed
     */
    @Transactional
    public Assessment assess(TransactionEvent event) {
        if (events.existsByTransactionId(event.transactionId())) {
            throw new DuplicateTransactionException(event.transactionId());
        }

        Instant now = clock.instant();
        List<TransactionEvent> history = events
                .findByPolicyNumberAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAsc(
                        event.policyNumber(),
                        event.occurredAt().minus(Duration.ofDays(FraudProperties.HISTORY_LOOKBACK_DAYS)),
                        event.occurredAt())
                .stream()
                .map(TransactionEventEntity::toDomain)
                .toList();

        ScoringResult result = engine.score(new RuleContext(event, history, now));

        // saveAndFlush surfaces unique-constraint violations from concurrent duplicate submissions here.
        TransactionEventEntity savedEvent = events.saveAndFlush(TransactionEventEntity.fromDomain(event, now));
        FraudAssessmentEntity saved = assessments.saveAndFlush(
                FraudAssessmentEntity.create(savedEvent, result, RULES_VERSION, now));

        meters.counter("fraud.assessments", "decision", result.decision().name(),
                "risk_level", result.riskLevel().name()).increment();
        log.info("Assessed transaction={} decision={} score={} rules={}",
                event.transactionId(), result.decision(), result.riskScore(), result.triggeredRules().size());

        return saved.toDomain();
    }

    @Transactional(readOnly = true)
    public Assessment getByTransactionId(String transactionId) {
        return assessments.findByEvent_TransactionId(transactionId)
                .map(FraudAssessmentEntity::toDomain)
                .orElseThrow(() -> new AssessmentNotFoundException(transactionId));
    }

    @Transactional(readOnly = true)
    public Page<Assessment> search(Decision decision, RiskLevel riskLevel, String policyNumber,
                                   Instant from, Instant to, Pageable pageable) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidQueryException("'from' must not be after 'to'");
        }
        return assessments.findAll(
                        AssessmentSpecifications.matching(decision, riskLevel, policyNumber, from, to), pageable)
                .map(FraudAssessmentEntity::toDomain);
    }
}
