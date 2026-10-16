package za.co.fraud.engine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;

/**
 * Externalised fraud rule parameters and scoring thresholds.
 * Validated at startup so a misconfiguration fails fast rather than producing wrong decisions.
 */
@ConfigurationProperties(prefix = "fraud")
public record FraudProperties(
        @DefaultValue("40") int reviewThreshold,
        @DefaultValue("70") int investigateThreshold,

        @DefaultValue("50000.00") BigDecimal highValueClaimThreshold,
        @DefaultValue("25") int highValueClaimWeight,

        @DefaultValue("3") int claimVelocityMaxClaims,
        @DefaultValue("30") int claimVelocityWindowDays,
        @DefaultValue("30") int claimVelocityWeight,

        @DefaultValue("10000.00") BigDecimal roundAmountMinimum,
        @DefaultValue("1000.00") BigDecimal roundAmountMultiple,
        @DefaultValue("10") int roundAmountWeight,

        @DefaultValue("30") int earlyClaimWindowDays,
        @DefaultValue("20") int earlyClaimWeight,

        @DefaultValue("14") int beneficiaryChangeWindowDays,
        @DefaultValue("25") int beneficiaryChangeWeight,

        @DefaultValue("7") int duplicateWindowDays,
        @DefaultValue("35") int duplicateClaimWeight) {

    /** How far back the engine looks at a policy's prior transactions. Rule windows must fit inside it. */
    public static final int HISTORY_LOOKBACK_DAYS = 90;

    public FraudProperties {
        if (reviewThreshold < 0 || investigateThreshold > 100 || reviewThreshold > investigateThreshold) {
            throw new IllegalArgumentException(
                    "Invalid thresholds: review=%d investigate=%d".formatted(reviewThreshold, investigateThreshold));
        }
        requireNonNegative("highValueClaimWeight", highValueClaimWeight);
        requireNonNegative("claimVelocityWeight", claimVelocityWeight);
        requireNonNegative("roundAmountWeight", roundAmountWeight);
        requireNonNegative("earlyClaimWeight", earlyClaimWeight);
        requireNonNegative("beneficiaryChangeWeight", beneficiaryChangeWeight);
        requireNonNegative("duplicateClaimWeight", duplicateClaimWeight);

        requireWindow("claimVelocityWindowDays", claimVelocityWindowDays);
        requireWindow("earlyClaimWindowDays", earlyClaimWindowDays);
        requireWindow("beneficiaryChangeWindowDays", beneficiaryChangeWindowDays);
        requireWindow("duplicateWindowDays", duplicateWindowDays);

        if (claimVelocityMaxClaims < 1) {
            throw new IllegalArgumentException("claimVelocityMaxClaims must be >= 1");
        }
        if (highValueClaimThreshold.signum() <= 0 || roundAmountMinimum.signum() <= 0) {
            throw new IllegalArgumentException("Monetary thresholds must be positive");
        }
        if (roundAmountMultiple.signum() <= 0) {
            throw new IllegalArgumentException("roundAmountMultiple must be positive");
        }
    }

    /** Defaults matching the {@code @DefaultValue} annotations; used by unit tests. */
    public static FraudProperties defaults() {
        return new FraudProperties(
                40, 70,
                new BigDecimal("50000.00"), 25,
                3, 30, 30,
                new BigDecimal("10000.00"), new BigDecimal("1000.00"), 10,
                30, 20,
                14, 25,
                7, 35);
    }

    private static void requireNonNegative(String name, int value) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be >= 0");
        }
    }

    private static void requireWindow(String name, int days) {
        if (days < 1 || days > HISTORY_LOOKBACK_DAYS) {
            throw new IllegalArgumentException(
                    "%s must be between 1 and %d days".formatted(name, HISTORY_LOOKBACK_DAYS));
        }
    }
}
