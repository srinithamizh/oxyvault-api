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
class SourceIntegrationTest {

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
    void userCanCreateListUpdateAndDeleteSourcesIncludingCashOnRequest() throws Exception {
        String token = registerAndLogin(uniqueName());

        mockMvc.perform(get("/sources").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        String created = mockMvc.perform(post("/sources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"type":"bank","name":"HDFC Bank","accountType":"savings",
                                 "lastFour":"4582","balance":248820.00}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("bank"))
                .andExpect(jsonPath("$.accountType").value("savings"))
                .andExpect(jsonPath("$.lastFour").value("4582"))
                .andExpect(jsonPath("$.balance").value(248820.00))
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String sourceId = JsonPath.read(created, "$.id");

        mockMvc.perform(post("/sources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"type":"cash","name":"Cash","balance":12450}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("cash"))
                .andExpect(jsonPath("$.name").value("Cash"));

        mockMvc.perform(put("/sources/{sourceId}", sourceId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"type":"card","name":"HDFC Regalia","lastFour":"8291",
                                 "creditLimit":500000,"balance":34680}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("card"))
                .andExpect(jsonPath("$.creditLimit").value(500000))
                .andExpect(jsonPath("$.balance").value(34680));

        mockMvc.perform(get("/sources").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(delete("/sources/{sourceId}", sourceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void usersCannotAccessAnotherUsersSources() throws Exception {
        String ownerToken = registerAndLogin(uniqueName());
        String otherToken = registerAndLogin(uniqueName());
        String created = mockMvc.perform(post("/sources")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"type":"loan","name":"Home Loan","provider":"SBI",
                                 "emi":32400,"interestRate":8.5,"balance":1842000}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.provider").value("SBI"))
                .andExpect(jsonPath("$.interestRate").value(8.5))
                .andReturn().getResponse().getContentAsString();
        String sourceId = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/sources").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(put("/sources/{sourceId}", sourceId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"type":"cash","name":"Cash","balance":100}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/sources/{sourceId}", sourceId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void sourceEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/sources")).andExpect(status().isUnauthorized());
    }

    @Test
    void sourceRejectsInvalidAmountsAndLastFour() throws Exception {
        String token = registerAndLogin(uniqueName());

        mockMvc.perform(post("/sources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"type":"card","name":"Card","lastFour":"12",
                                 "creditLimit":-1,"balance":0}
                                """))
                .andExpect(status().isBadRequest());
    }
}
