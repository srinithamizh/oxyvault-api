package in.srinithamizh.oxyvault;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransactionIntegrationTest {

    private static final String PASSWORD = "Str0ng!Pass";

    @Autowired
    private MockMvc mockMvc;

    private static String uniqueName() {
        return "user" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String registerAndLogin(String username) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"firstName":"Test","lastName":"User","username":"%s",
                                 "email":"%s@example.com","password":"%s"}
                                """.formatted(username, username, PASSWORD)))
                .andExpect(status().isCreated());

        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"identifier":"%s","password":"%s"}
                                """.formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(loginResponse, "$.accessToken");
    }

    @Test
    void userCanCreateAndListTransactionsWithEntityReferencesAndAdditionalInfo() throws Exception {
        String token = registerAndLogin(uniqueName());
        String sourceId = createSource(token);
        String paymentId = createPayment(token);
        String categoryId = createCategory(token);

        mockMvc.perform(get("/transactions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page.size").value(25))
                .andExpect(jsonPath("$.page.number").value(0));

        String response = mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"description":" Lunch ","amount":250.50,"sourceId":"%s",
                                 "paymentId":"%s","categoryId":"%s","currency":"INR",
                                 "additionalInfo":"Merchant: Cafe; receipt: 42"}
                                """.formatted(sourceId, paymentId, categoryId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Lunch"))
                .andExpect(jsonPath("$.amount").value(250.50))
                .andExpect(jsonPath("$.sourceId").value(sourceId))
                .andExpect(jsonPath("$.paymentId").value(paymentId))
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.currency").value("INR"))
                .andExpect(jsonPath("$.additionalInfo").value("Merchant: Cafe; receipt: 42"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String transactionId = JsonPath.read(response, "$.id");

        mockMvc.perform(get("/transactions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(transactionId))
                .andExpect(jsonPath("$.page.totalElements").value(1));

        mockMvc.perform(delete("/transactions/{transactionId}", transactionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/transactions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void transactionsArePagedByNewestCreatedAtAndCanBeFiltered() throws Exception {
        String token = registerAndLogin(uniqueName());
        String sourceId = createSource(token, "10000");
        String paymentId = createPayment(token);
        String categoryId = createCategory(token);
        String firstTransaction = createTransaction(token, sourceId, paymentId, categoryId);
        String firstId = JsonPath.read(firstTransaction, "$.id");
        String firstCreatedAt = JsonPath.read(firstTransaction, "$.createdAt");
        for (int i = 0; i < 25; i++) {
            createTransaction(token, sourceId, paymentId, categoryId);
        }

        mockMvc.perform(get("/transactions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(25))
                .andExpect(jsonPath("$.page.size").value(25))
                .andExpect(jsonPath("$.page.totalElements").value(26))
                .andExpect(jsonPath("$.page.totalPages").value(2))
                .andExpect(jsonPath("$.page.number").value(0));

        mockMvc.perform(get("/transactions")
                        .header("Authorization", "Bearer " + token)
                        .param("page", "1")
                        .param("size", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page.number").value(1));

        mockMvc.perform(get("/transactions")
                        .header("Authorization", "Bearer " + token)
                        .param("sourceId", sourceId)
                        .param("paymentId", paymentId)
                        .param("categoryId", categoryId)
                        .param("from", firstCreatedAt)
                        .param("to", firstCreatedAt)
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(firstId))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void transactionPaginationRejectsInvalidPageSizeAndDateRange() throws Exception {
        String token = registerAndLogin(uniqueName());

        mockMvc.perform(get("/transactions")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "101"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/transactions")
                        .header("Authorization", "Bearer " + token)
                        .param("from", "2026-10-22T00:00:00Z")
                        .param("to", "2026-10-21T00:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transactionCreateUpdateAndDeleteKeepSourceBalanceConsistent() throws Exception {
        String token = registerAndLogin(uniqueName());
        String sourceId = createSource(token, "500");
        String paymentId = createPayment(token);
        String expenseCategoryId = createCategory(token, "EXPENSE");
        String incomeCategoryId = createCategory(token, "INCOME");

        String transactionResponse = createTransaction(token, sourceId, paymentId, expenseCategoryId, "200");
        String transactionId = JsonPath.read(transactionResponse, "$.id");
        assertSourceBalance(token, 300);

        mockMvc.perform(put("/transactions/{transactionId}", transactionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"description":"Reduced expense","amount":150,"sourceId":"%s",
                                 "paymentId":"%s","categoryId":"%s","currency":"INR"}
                                """.formatted(sourceId, paymentId, expenseCategoryId)))
                .andExpect(status().isOk());
        assertSourceBalance(token, 350);

        mockMvc.perform(put("/transactions/{transactionId}", transactionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"description":"Increased expense","amount":250,"sourceId":"%s",
                                 "paymentId":"%s","categoryId":"%s","currency":"INR"}
                                """.formatted(sourceId, paymentId, expenseCategoryId)))
                .andExpect(status().isOk());
        assertSourceBalance(token, 250);

        String exactBalanceTransaction = createTransaction(
                token, sourceId, paymentId, expenseCategoryId, "250"
        );
        String exactBalanceTransactionId = JsonPath.read(exactBalanceTransaction, "$.id");
        assertSourceBalance(token, 0);

        mockMvc.perform(put("/categories/{categoryId}", expenseCategoryId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Food EXPENSE\",\"type\":\"INCOME\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"description":"Overspend","amount":1,"sourceId":"%s",
                                 "paymentId":"%s","categoryId":"%s","currency":"INR"}
                                """.formatted(sourceId, paymentId, expenseCategoryId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Insufficient balance in the selected source to record this transaction."
                ));
        assertSourceBalance(token, 0);

        mockMvc.perform(put("/transactions/{transactionId}", transactionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"description":"Refund","amount":50,"sourceId":"%s",
                                 "paymentId":"%s","categoryId":"%s","currency":"INR"}
                                """.formatted(sourceId, paymentId, incomeCategoryId)))
                .andExpect(status().isOk());
        assertSourceBalance(token, 300);

        mockMvc.perform(put("/transactions/{transactionId}", transactionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"description":"Too large expense","amount":251,"sourceId":"%s",
                                 "paymentId":"%s","categoryId":"%s","currency":"INR"}
                                """.formatted(sourceId, paymentId, expenseCategoryId)))
                .andExpect(status().isConflict());
        assertSourceBalance(token, 300);

        mockMvc.perform(delete("/transactions/{transactionId}", transactionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        assertSourceBalance(token, 250);
        mockMvc.perform(delete("/transactions/{transactionId}", exactBalanceTransactionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        assertSourceBalance(token, 500);
    }

    @Test
    void transactionsRequireOwnedReferences() throws Exception {
        String ownerToken = registerAndLogin(uniqueName());
        String otherToken = registerAndLogin(uniqueName());
        String sourceId = createSource(ownerToken);
        String paymentId = createPayment(ownerToken);
        String categoryId = createCategory(ownerToken);

        mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"description":"Lunch","amount":250.50,"sourceId":"%s",
                                 "paymentId":"%s","categoryId":"%s","currency":"INR"}
                                """.formatted(sourceId, paymentId, categoryId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotDeleteSourcePaymentOrCategoryUsedByTransaction() throws Exception {
        String token = registerAndLogin(uniqueName());
        String sourceId = createSource(token);
        String paymentId = createPayment(token);
        String categoryId = createCategory(token);

        String transactionResponse = mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"description":"Lunch","amount":250.50,"sourceId":"%s",
                                 "paymentId":"%s","categoryId":"%s","currency":"INR"}
                                """.formatted(sourceId, paymentId, categoryId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String transactionId = JsonPath.read(transactionResponse, "$.id");

        mockMvc.perform(delete("/sources/{sourceId}", sourceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "You can't delete this source because it is recorded in a transaction. "
                                + "Update the transaction and then delete this source."
                ));

        mockMvc.perform(delete("/payments/{paymentId}", paymentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "You can't delete this payment method because it is recorded in a transaction. "
                                + "Update the transaction and then delete this payment method."
                ));

        mockMvc.perform(delete("/categories/{categoryId}", categoryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "You can't delete this category because it is recorded in a transaction. "
                                + "Update the transaction and then delete this category."
                ));

        String replacementSourceId = createSource(token);
        String replacementPaymentId = createPayment(token);
        String replacementCategoryId = createCategory(token);
        mockMvc.perform(put("/transactions/{transactionId}", transactionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"description":"Lunch","amount":250.50,"sourceId":"%s",
                                 "paymentId":"%s","categoryId":"%s","currency":"INR"}
                                """.formatted(replacementSourceId, replacementPaymentId, replacementCategoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceId").value(replacementSourceId))
                .andExpect(jsonPath("$.paymentId").value(replacementPaymentId))
                .andExpect(jsonPath("$.categoryId").value(replacementCategoryId));

        mockMvc.perform(delete("/sources/{sourceId}", sourceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/payments/{paymentId}", paymentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/categories/{categoryId}", categoryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void transactionEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/transactions")).andExpect(status().isUnauthorized());
    }

    @Test
    void usersCannotDeleteAnotherUsersTransaction() throws Exception {
        String ownerToken = registerAndLogin(uniqueName());
        String otherToken = registerAndLogin(uniqueName());
        String sourceId = createSource(ownerToken);
        String paymentId = createPayment(ownerToken);
        String categoryId = createCategory(ownerToken);
        String response = mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"description":"Lunch","amount":250.50,"sourceId":"%s",
                                 "paymentId":"%s","categoryId":"%s","currency":"INR"}
                                """.formatted(sourceId, paymentId, categoryId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String transactionId = JsonPath.read(response, "$.id");

        mockMvc.perform(delete("/transactions/{transactionId}", transactionId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    private String createSource(String token) throws Exception {
        return createSource(token, "1000");
    }

    private String createSource(String token, String balance) throws Exception {
        String response = mockMvc.perform(post("/sources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"type":"cash","name":"Cash","balance":%s}
                                """.formatted(balance)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String createPayment(String token) throws Exception {
        String response = mockMvc.perform(post("/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"UPI\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String createCategory(String token) throws Exception {
        return createCategory(token, "EXPENSE");
    }

    private String createCategory(String token, String type) throws Exception {
        String response = mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Food %s\",\"type\":\"%s\"}".formatted(type, type)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private String createTransaction(
            String token,
            String sourceId,
            String paymentId,
            String categoryId,
            String amount
    ) throws Exception {
        return mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"description":"Transaction","amount":%s,"sourceId":"%s",
                                 "paymentId":"%s","categoryId":"%s","currency":"INR"}
                                """.formatted(amount, sourceId, paymentId, categoryId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private void assertSourceBalance(String token, double expectedBalance) throws Exception {
        mockMvc.perform(get("/sources").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].balance").value(expectedBalance));
    }

    private String createTransaction(String token, String sourceId, String paymentId, String categoryId)
            throws Exception {
        return mockMvc.perform(post("/transactions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"description":"Lunch","amount":250.50,"sourceId":"%s",
                                 "paymentId":"%s","categoryId":"%s","currency":"INR"}
                                """.formatted(sourceId, paymentId, categoryId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }
}
