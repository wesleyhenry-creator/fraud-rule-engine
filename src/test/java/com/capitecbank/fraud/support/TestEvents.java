package com.capitecbank.fraud.support;

import com.capitecbank.fraud.domain.TransactionCategory;
import com.capitecbank.fraud.domain.TransactionEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class TestEvents {

    private TestEvents() {
    }

    public static TransactionEvent event(Instant occurredAt, TransactionCategory category, String amount) {
        return event(occurredAt, category, amount, null);
    }

    public static TransactionEvent event(Instant occurredAt, TransactionCategory category, String amount,
                                         LocalDate policyInceptionDate) {
        return new TransactionEvent(
                "TX-" + UUID.randomUUID(),
                "POL-1",
                "ACC-000111222",
                null,
                category,
                new BigDecimal(amount),
                occurredAt,
                null,
                policyInceptionDate,
                occurredAt);
    }
}
