package za.co.fraud.engine.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface TransactionEventRepository extends JpaRepository<TransactionEventEntity, Long> {

    boolean existsByTransactionId(String transactionId);

    /** Prior transactions on a policy in [from, to), oldest first. */
    List<TransactionEventEntity> findByPolicyNumberAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtAsc(
            String policyNumber, Instant from, Instant to);
}
