package za.co.fraud.engine.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface FraudAssessmentRepository
        extends JpaRepository<FraudAssessmentEntity, UUID>, JpaSpecificationExecutor<FraudAssessmentEntity> {

    Optional<FraudAssessmentEntity> findByEvent_TransactionId(String transactionId);
}
