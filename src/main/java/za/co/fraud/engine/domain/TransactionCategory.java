package za.co.fraud.engine.domain;

/** Category assigned upstream to each transaction event. */
public enum TransactionCategory {
    PREMIUM_PAYMENT,
    CLAIM_SUBMISSION,
    CLAIM_PAYOUT,
    BENEFICIARY_UPDATE,
    POLICY_CHANGE;

    public boolean isClaim() {
        return this == CLAIM_SUBMISSION || this == CLAIM_PAYOUT;
    }
}
