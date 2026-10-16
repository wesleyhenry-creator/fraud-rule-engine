package za.co.fraud.engine.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import za.co.fraud.engine.api.dto.TransactionRequest;
import za.co.fraud.engine.domain.TransactionCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FraudApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private static String uniq(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static TransactionRequest request(String txId, String policy, TransactionCategory category,
                                              String amount, Instant at, String payee, LocalDate inception) {
        return new TransactionRequest(txId, policy, "ACC-" + policy, payee, category,
                new BigDecimal(amount), at, null, inception);
    }

    private String submit(TransactionRequest req) throws Exception {
        return mvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void cleanPremiumPaymentIsCleared() throws Exception {
        var req = request(uniq("TX"), uniq("POL"), TransactionCategory.PREMIUM_PAYMENT, "1500.00",
                Instant.parse("2026-10-10T10:00:00Z"), "INS-HOUSE", null);

        mvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.decision").value("CLEAR"))
                .andExpect(jsonPath("$.riskScore").value(0))
                .andExpect(jsonPath("$.riskLevel").value("LOW"));
    }

    @Test
    void sequenceOfBeneficiaryChangeThenLargeRoundEarlyPayoutIsFlagged() throws Exception {
        String policy = uniq("POL");
        var inception = LocalDate.of(2026, 10, 1);

        mvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request(uniq("TX"), policy,
                                TransactionCategory.BENEFICIARY_UPDATE, "0.00",
                                Instant.parse("2026-10-05T09:00:00Z"), "NEW-ACC", inception))))
                .andExpect(status().isCreated());

        String txId = uniq("TX");
        mvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(request(txId, policy,
                                TransactionCategory.CLAIM_PAYOUT, "60000.00",
                                Instant.parse("2026-10-10T10:00:00Z"), "NEW-ACC", inception))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.riskScore").value(80))
                .andExpect(jsonPath("$.riskLevel").value("HIGH"))
                .andExpect(jsonPath("$.decision").value("FLAG_FOR_INVESTIGATION"))
                .andExpect(jsonPath("$.triggeredRules.length()").value(4));

        mvc.perform(get("/api/v1/assessments/{id}", txId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(txId))
                .andExpect(jsonPath("$.decision").value("FLAG_FOR_INVESTIGATION"));
    }

    @Test
    void resubmittingTransactionIdReturnsConflict() throws Exception {
        var req = request(uniq("TX"), uniq("POL"), TransactionCategory.PREMIUM_PAYMENT, "900.00",
                Instant.parse("2026-10-10T10:00:00Z"), "INS-HOUSE", null);

        mvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate transaction"));
    }

    @Test
    void invalidPayloadReturnsBadRequestWithFieldErrors() throws Exception {
        String body = """
                {"transactionId":"","policyNumber":"P","accountId":"A","payeeAccountId":"B",
                 "category":"CLAIM_PAYOUT","amount":-5,"occurredAt":"2026-10-10T10:00:00Z"}
                """;
        mvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void unknownCategoryIsRejected() throws Exception {
        String body = """
                {"transactionId":"X1","policyNumber":"P","accountId":"A","payeeAccountId":"B",
                 "category":"BOGUS","amount":10,"occurredAt":"2026-10-10T10:00:00Z"}
                """;
        mvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownAssessmentReturnsNotFound() throws Exception {
        mvc.perform(get("/api/v1/assessments/{id}", "does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Assessment not found"));
    }

    @Test
    void searchFiltersByDecisionAndReturnsPage() throws Exception {
        String policy = uniq("POL");
        submit(request(uniq("TX"), policy, TransactionCategory.PREMIUM_PAYMENT, "100.00",
                Instant.parse("2026-10-10T10:00:00Z"), "INS-HOUSE", null));

        mvc.perform(get("/api/v1/assessments")
                        .param("decision", "CLEAR")
                        .param("policyNumber", policy))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].policyNumber").value(policy));
    }

    @Test
    void searchRejectsInvertedDateRange() throws Exception {
        mvc.perform(get("/api/v1/assessments")
                        .param("from", "2026-10-12T00:00:00Z")
                        .param("to", "2026-10-01T00:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rulesEndpointListsActiveRules() throws Exception {
        mvc.perform(get("/api/v1/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].code").value("CLAIM_VELOCITY"));
    }
}
