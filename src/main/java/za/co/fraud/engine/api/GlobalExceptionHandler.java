package za.co.fraud.engine.api;

import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import za.co.fraud.engine.service.AssessmentNotFoundException;
import za.co.fraud.engine.service.DuplicateTransactionException;
import za.co.fraud.engine.service.InvalidQueryException;

/** Maps domain and persistence failures to RFC 7807 problem responses. Validation errors come from the base class. */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(DuplicateTransactionException.class)
    public ProblemDetail handleDuplicate(DuplicateTransactionException ex) {
        return problem(HttpStatus.CONFLICT, "Duplicate transaction", ex.getMessage());
    }

    /** Covers a concurrent duplicate submission that passed the existence check but hit the unique constraint. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegrity(DataIntegrityViolationException ex) {
        return problem(HttpStatus.CONFLICT, "Conflicting data",
                "The request conflicts with existing data, most likely a duplicate transaction id");
    }

    @ExceptionHandler(AssessmentNotFoundException.class)
    public ProblemDetail handleNotFound(AssessmentNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Assessment not found", ex.getMessage());
    }

    @ExceptionHandler({InvalidQueryException.class, ConstraintViolationException.class})
    public ProblemDetail handleBadQuery(RuntimeException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", ex.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
