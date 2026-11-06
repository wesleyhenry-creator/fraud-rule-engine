package com.capitecbank.fraud.error;

public class AssessmentNotFoundException extends RuntimeException {

    public AssessmentNotFoundException(String transactionId) {
        super("No fraud assessment found for transaction: " + transactionId);
    }
}
