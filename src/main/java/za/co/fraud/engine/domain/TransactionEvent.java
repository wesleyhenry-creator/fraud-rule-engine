package za.co.fraud.engine.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A categorised transaction against an insurance policy. Amounts are in ZAR.
 *
 * @param accountId        the policyholder's bank account
 * @param payeeAccountId   the account receiving funds (payouts) or the new beneficiary account (updates)
 * @param claimReference   claim identifier supplied by the upstream claims system, if any
 * @param policyInceptionDate date the policy started, if known
 */
public record TransactionEvent(
        String transactionId,
        String policyNumber,
        String accountId,
        String payeeAccountId,
        TransactionCategory category,
        BigDecimal amount,
        Instant occurredAt,
        String claimReference,
        LocalDate policyInceptionDate) {
}
