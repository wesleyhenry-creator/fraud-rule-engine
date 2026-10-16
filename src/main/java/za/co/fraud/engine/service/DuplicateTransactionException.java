package za.co.fraud.engine.service;

public class DuplicateTransactionException extends RuntimeException {

    public DuplicateTransactionException(String transactionId) {
        super("Transaction '%s' has already been assessed".formatted(transactionId));
    }
}
