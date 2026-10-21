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
class PaymentMethodIntegrationTest {

    private static final String PASSWORD = "Str0ng!Pass";

    @Autowired
    private MockMvc mockMvc;

    private static String uniqueName() {
        return "user" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String registerAndLogin(String username) throws Exception {
        String registrationBody = """
                {"firstName":"Test","lastName":"User","username":"%s","email":"%s@example.com","password":"%s"}
                """.formatted(username, username, PASSWORD);
        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(registrationBody))
                .andExpect(status().isCreated());

        String loginBody = """
                {"identifier":"%s","password":"%s"}
                """.formatted(username, PASSWORD);
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    @Test
    void userCanCreateListUpdateAndDeleteOwnPaymentMethods() throws Exception {
        String token = registerAndLogin(uniqueName());

        mockMvc.perform(get("/payments").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        String created = mockMvc.perform(post("/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"UPI\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("UPI"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String paymentMethodId = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/payments").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(paymentMethodId))
                .andExpect(jsonPath("$[0].name").value("UPI"));

        mockMvc.perform(put("/payments/{paymentMethodId}", paymentMethodId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Bank transfer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(paymentMethodId))
                .andExpect(jsonPath("$.name").value("Bank transfer"));

        mockMvc.perform(delete("/payments/{paymentMethodId}", paymentMethodId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/payments").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void usersCannotViewOrModifyAnotherUsersPaymentMethods() throws Exception {
        String ownerToken = registerAndLogin(uniqueName());
        String otherToken = registerAndLogin(uniqueName());
        String created = mockMvc.perform(post("/payments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Cash\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String paymentMethodId = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/payments").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(put("/payments/{paymentMethodId}", paymentMethodId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Updated\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/payments/{paymentMethodId}", paymentMethodId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void paymentMethodsRequireAuthenticationAndNonBlankNames() throws Exception {
        mockMvc.perform(get("/payments")).andExpect(status().isUnauthorized());

        String token = registerAndLogin(uniqueName());
        mockMvc.perform(post("/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }
}
