package com.capitecbank.fraud.persistence;

import com.capitecbank.fraud.domain.Decision;
import com.capitecbank.fraud.domain.FraudAssessment;
import com.capitecbank.fraud.domain.RiskLevel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FraudAssessmentRepository extends JpaRepository<FraudAssessment, UUID> {

    @EntityGraph(attributePaths = "transactionEvent")
    Optional<FraudAssessment> findByTransactionEventTransactionId(String transactionId);

    @EntityGraph(attributePaths = "transactionEvent")
    @Query("""
            select a from FraudAssessment a
            where (:decision is null or a.decision = :decision)
              and (:riskLevel is null or a.riskLevel = :riskLevel)
            order by a.evaluatedAt desc
            """)
    Page<FraudAssessment> search(@Param("decision") Decision decision,
                                 @Param("riskLevel") RiskLevel riskLevel,
                                 Pageable pageable);
}
