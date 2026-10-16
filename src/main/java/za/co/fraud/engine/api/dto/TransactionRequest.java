package za.co.fraud.engine.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import za.co.fraud.engine.domain.TransactionCategory;
import za.co.fraud.engine.domain.TransactionEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Schema(description = "A categorised transaction event to assess. Amounts are in ZAR.")
public record TransactionRequest(
        @NotBlank @Size(max = 64) String transactionId,
        @NotBlank @Size(max = 40) String policyNumber,
        @NotBlank @Size(max = 40) String accountId,
        @NotBlank @Size(max = 40) String payeeAccountId,
        @NotNull TransactionCategory category,
        @NotNull @DecimalMin("0.00") @Digits(integer = 13, fraction = 2) BigDecimal amount,
        @NotNull Instant occurredAt,
        @Size(max = 64) String claimReference,
        @PastOrPresent LocalDate policyInceptionDate) {

    public TransactionEvent toDomain() {
        return new TransactionEvent(
                transactionId,
                policyNumber,
                accountId,
                payeeAccountId,
                category,
                amount.setScale(2),
                occurredAt,
                claimReference,
                policyInceptionDate);
    }
}
