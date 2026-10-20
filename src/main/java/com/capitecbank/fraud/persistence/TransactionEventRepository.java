package com.capitecbank.fraud.persistence;

import com.capitecbank.fraud.domain.TransactionEvent;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionEventRepository extends JpaRepository<TransactionEvent, Long> {

    boolean existsByTransactionId(String transactionId);

    List<TransactionEvent> findByPolicyNumberAndOccurredAtGreaterThanEqualAndOccurredAtBeforeOrderByOccurredAtAsc(
            String policyNumber, Instant from, Instant before);
}
