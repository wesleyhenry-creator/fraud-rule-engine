package com.capitecbank.fraud.api;

import com.capitecbank.fraud.domain.TransactionCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record TransactionEventRequest(
        @NotBlank @Size(max = 64) String transactionId,
        @NotBlank @Size(max = 50) String policyNumber,
        @NotBlank @Size(max = 50) String accountId,
        @Size(max = 50) String payeeAccountId,
        @NotNull TransactionCategory category,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 13, fraction = 2) BigDecimal amount,
        @NotNull Instant occurredAt,
        @Size(max = 64) String claimReference,
        @PastOrPresent LocalDate policyInceptionDate) {
}
