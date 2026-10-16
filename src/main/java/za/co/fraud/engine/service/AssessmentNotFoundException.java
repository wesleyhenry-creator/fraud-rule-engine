package za.co.fraud.engine.service;

public class AssessmentNotFoundException extends RuntimeException {

    public AssessmentNotFoundException(String transactionId) {
        super("No assessment found for transaction '%s'".formatted(transactionId));
    }
}
