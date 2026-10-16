package za.co.fraud.engine.rules;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import za.co.fraud.engine.config.FraudProperties;
import za.co.fraud.engine.domain.RuleOutcome;
import za.co.fraud.engine.domain.TransactionCategory;
import za.co.fraud.engine.domain.TransactionEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class FraudRulesTest {

    private static final FraudProperties PROPS = FraudProperties.defaults();
    private static final Instant NOW = Instant.parse("2026-10-12T08:00:00Z");
    private static final Instant T = Instant.parse("2026-10-10T10:00:00Z");

    private static TransactionEvent tx(String id, TransactionCategory category, String amount, Instant at,
                                       String payee, String claimRef, LocalDate inception) {
        return new TransactionEvent(id, "POL-1", "ACC-1", payee, category,
                new BigDecimal(amount), at, claimRef, inception);
    }

    private static TransactionEvent claim(String id, String amount, Instant at, String payee) {
        return tx(id, TransactionCategory.CLAIM_PAYOUT, amount, at, payee, null, null);
    }

    private static RuleContext ctx(TransactionEvent current, TransactionEvent... history) {
        return new RuleContext(current, List.of(history), NOW);
    }

    private static boolean fires(FraudRule rule, RuleContext context) {
        Optional<RuleOutcome> outcome = rule.evaluate(context);
        return outcome.isPresent();
    }

    @Nested
    class HighValueClaim {
        private final FraudRule rule = new HighValueClaimRule(PROPS);

        @Test
        void firesAtThreshold() {
            assertThat(fires(rule, ctx(claim("t1", "50000.00", T, "A")))).isTrue();
        }

        @Test
        void doesNotFireBelowThreshold() {
            assertThat(fires(rule, ctx(claim("t1", "49999.99", T, "A")))).isFalse();
        }

        @Test
        void ignoresNonClaimCategories() {
            var premium = tx("t1", TransactionCategory.PREMIUM_PAYMENT, "60000.00", T, "A", null, null);
            assertThat(fires(rule, ctx(premium))).isFalse();
        }

        @Test
        void reportsWeightAndCode() {
            var outcome = rule.evaluate(ctx(claim("t1", "60000.00", T, "A"))).orElseThrow();
            assertThat(outcome.ruleCode()).isEqualTo("HIGH_VALUE_CLAIM");
            assertThat(outcome.weight()).isEqualTo(25);
        }
    }

    @Nested
    class ClaimVelocity {
        private final FraudRule rule = new ClaimVelocityRule(PROPS);

        @Test
        void firesWhenPriorClaimsReachLimit() {
            var history = new TransactionEvent[]{
                    claim("h1", "1000.00", T.minusSeconds(1 * 86400), "A"),
                    claim("h2", "1000.00", T.minusSeconds(2 * 86400), "A"),
                    claim("h3", "1000.00", T.minusSeconds(3 * 86400), "A")};
            assertThat(fires(rule, ctx(claim("t1", "1000.00", T, "A"), history))).isTrue();
        }

        @Test
        void doesNotFireBelowLimit() {
            var history = new TransactionEvent[]{
                    claim("h1", "1000.00", T.minusSeconds(1 * 86400), "A"),
                    claim("h2", "1000.00", T.minusSeconds(2 * 86400), "A")};
            assertThat(fires(rule, ctx(claim("t1", "1000.00", T, "A"), history))).isFalse();
        }

        @Test
        void ignoresClaimsOutsideWindow() {
            var history = new TransactionEvent[]{
                    claim("h1", "1000.00", T.minusSeconds(40L * 86400), "A"),
                    claim("h2", "1000.00", T.minusSeconds(41L * 86400), "A"),
                    claim("h3", "1000.00", T.minusSeconds(42L * 86400), "A")};
            assertThat(fires(rule, ctx(claim("t1", "1000.00", T, "A"), history))).isFalse();
        }
    }

    @Nested
    class RoundAmountClaim {
        private final FraudRule rule = new RoundAmountClaimRule(PROPS);

        @Test
        void firesForLargeRoundAmount() {
            assertThat(fires(rule, ctx(claim("t1", "60000.00", T, "A")))).isTrue();
        }

        @Test
        void doesNotFireForNonRoundAmount() {
            assertThat(fires(rule, ctx(claim("t1", "60500.00", T, "A")))).isFalse();
        }

        @Test
        void doesNotFireBelowMinimum() {
            assertThat(fires(rule, ctx(claim("t1", "5000.00", T, "A")))).isFalse();
        }
    }

    @Nested
    class EarlyClaim {
        private final FraudRule rule = new EarlyClaimRule(PROPS);

        @Test
        void firesShortlyAfterInception() {
            var t = tx("t1", TransactionCategory.CLAIM_SUBMISSION, "1500.00", T, "A", null, LocalDate.of(2026, 9, 30));
            assertThat(fires(rule, ctx(t))).isTrue();
        }

        @Test
        void doesNotFireForEstablishedPolicy() {
            var t = tx("t1", TransactionCategory.CLAIM_SUBMISSION, "1500.00", T, "A", null, LocalDate.of(2026, 8, 11));
            assertThat(fires(rule, ctx(t))).isFalse();
        }

        @Test
        void doesNotFireWhenInceptionUnknown() {
            var t = tx("t1", TransactionCategory.CLAIM_SUBMISSION, "1500.00", T, "A", null, null);
            assertThat(fires(rule, ctx(t))).isFalse();
        }
    }

    @Nested
    class RecentBeneficiaryChangePayout {
        private final FraudRule rule = new RecentBeneficiaryChangePayoutRule(PROPS);

        @Test
        void firesWhenPayoutGoesToRecentlyNominatedAccount() {
            var update = tx("u1", TransactionCategory.BENEFICIARY_UPDATE, "0.00", T.minusSeconds(3 * 86400), "NEW-ACC", null, null);
            assertThat(fires(rule, ctx(claim("t1", "60000.00", T, "NEW-ACC"), update))).isTrue();
        }

        @Test
        void doesNotFireWhenPayeeDiffersFromNominee() {
            var update = tx("u1", TransactionCategory.BENEFICIARY_UPDATE, "0.00", T.minusSeconds(3 * 86400), "NEW-ACC", null, null);
            assertThat(fires(rule, ctx(claim("t1", "60000.00", T, "OTHER-ACC"), update))).isFalse();
        }

        @Test
        void doesNotFireWhenChangeIsOutsideWindow() {
            var update = tx("u1", TransactionCategory.BENEFICIARY_UPDATE, "0.00", T.minusSeconds(20 * 86400), "NEW-ACC", null, null);
            assertThat(fires(rule, ctx(claim("t1", "60000.00", T, "NEW-ACC"), update))).isFalse();
        }
    }

    @Nested
    class DuplicateClaim {
        private final FraudRule rule = new DuplicateClaimRule(PROPS);

        @Test
        void firesOnSharedClaimReference() {
            var prior = tx("h1", TransactionCategory.CLAIM_SUBMISSION, "5000.00", T.minusSeconds(2 * 86400), "A", "CLM-1", null);
            var current = tx("t1", TransactionCategory.CLAIM_SUBMISSION, "7000.00", T, "B", "CLM-1", null);
            assertThat(fires(rule, ctx(current, prior))).isTrue();
        }

        @Test
        void firesOnSameAmountAndPayee() {
            var prior = claim("h1", "5000.00", T.minusSeconds(5 * 86400), "A");
            assertThat(fires(rule, ctx(claim("t1", "5000.00", T, "A"), prior))).isTrue();
        }

        @Test
        void doesNotFireOutsideWindow() {
            var prior = claim("h1", "5000.00", T.minusSeconds(10 * 86400), "A");
            assertThat(fires(rule, ctx(claim("t1", "5000.00", T, "A"), prior))).isFalse();
        }
    }
}
