package za.co.fraud.engine.persistence;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import za.co.fraud.engine.domain.Decision;
import za.co.fraud.engine.domain.RiskLevel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class AssessmentSpecifications {

    private AssessmentSpecifications() {
    }

    /** Builds a filter from optional criteria; null criteria are ignored. */
    public static Specification<FraudAssessmentEntity> matching(Decision decision, RiskLevel riskLevel,
                                                                String policyNumber, Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (decision != null) {
                predicates.add(cb.equal(root.get("decision"), decision));
            }
            if (riskLevel != null) {
                predicates.add(cb.equal(root.get("riskLevel"), riskLevel));
            }
            if (policyNumber != null) {
                predicates.add(cb.equal(root.join("event").get("policyNumber"), policyNumber));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("evaluatedAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThan(root.get("evaluatedAt"), to));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
