package com.capitecbank.fraud.error;

public class DuplicateTransactionException extends RuntimeException {

    public DuplicateTransactionException(String transactionId) {
        super("Transaction already exists: " + transactionId);
    }
}
