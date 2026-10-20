package com.capitecbank.fraud.api;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FraudAssessmentApiTest {

    private static final String CLAIM_TEMPLATE = """
            {
              "transactionId": "%s",
              "policyNumber": "POL-9001",
              "accountId": "ZA1234567890",
              "payeeAccountId": "ZA0987654321",
              "category": "CLAIM_PAYOUT",
              "amount": 60000.00,
              "occurredAt": "2026-10-20T08:00:00Z",
              "claimReference": "CLM-1",
              "policyInceptionDate": "2026-10-10"
            }
            """;

    @Autowired
    private MockMvc mvc;

    @Test
    void rulesEndpointListsRegisteredRules() throws Exception {
        mvc.perform(get("/api/v1/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItems("VELOCITY", "EARLY_CLAIM", "HIGH_AMOUNT")));
    }

    @Test
    void submittedClaimIsAssessedMaskedAndRetrievable() throws Exception {
        mvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CLAIM_TEMPLATE.formatted("TX-E2E-001")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/transactions/TX-E2E-001/assessment")))
                .andExpect(jsonPath("$.accountId").value("********7890"))
                .andExpect(jsonPath("$.payeeAccountId").value("********4321"))
                .andExpect(jsonPath("$.riskScore").value(40))
                .andExpect(jsonPath("$.riskLevel").value("MEDIUM"))
                .andExpect(jsonPath("$.decision").value("REFER_FOR_REVIEW"))
                .andExpect(jsonPath("$.rules[*].ruleCode", hasItems("EARLY_CLAIM", "HIGH_AMOUNT")));

        mvc.perform(get("/api/v1/transactions/TX-E2E-001/assessment"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("REFER_FOR_REVIEW"));

        mvc.perform(get("/api/v1/assessments").param("decision", "REFER_FOR_REVIEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].transactionId").value("TX-E2E-001"));
    }

    @Test
    void duplicateTransactionIdReturnsConflict() throws Exception {
        String body = CLAIM_TEMPLATE.formatted("TX-E2E-002");
        mvc.perform(post("/api/v1/transactions").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/transactions").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void invalidAmountReturnsValidationErrors() throws Exception {
        String body = CLAIM_TEMPLATE.formatted("TX-E2E-003").replace("60000.00", "-5");
        mvc.perform(post("/api/v1/transactions").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.violations[0].field").value("amount"));
    }

    @Test
    void unknownTransactionReturnsNotFound() throws Exception {
        mvc.perform(get("/api/v1/transactions/does-not-exist/assessment"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
