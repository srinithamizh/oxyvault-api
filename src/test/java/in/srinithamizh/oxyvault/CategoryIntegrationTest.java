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
class CategoryIntegrationTest {

    private static final String PASSWORD = "Str0ng!Pass";

    @Autowired
    private MockMvc mockMvc;

    private static String uniqueName() {
        return "user" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String registerAndLogin(String username) throws Exception {
        String body = """
                {"firstName":"Test","lastName":"User","username":"%s","email":"%s@example.com","password":"%s"}
                """.formatted(username, username, PASSWORD);
        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(body))
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
    void userCanCreateListUpdateAndDeleteOwnCategories() throws Exception {
        String username = uniqueName();
        String token = registerAndLogin(username);

        String created = mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\" Salary \",\"type\":\"INCOME\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Salary"))
                .andExpect(jsonPath("$.type").value("INCOME"))
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String categoryId = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/categories").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(categoryId))
                .andExpect(jsonPath("$[0].name").value("Salary"));

        mockMvc.perform(get("/categories")
                        .header("Authorization", "Bearer " + token)
                        .param("type", "EXPENSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        String expenseCategory = mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Food\",\"type\":\"EXPENSE\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String expenseCategoryId = JsonPath.read(expenseCategory, "$.id");
        mockMvc.perform(get("/categories")
                        .header("Authorization", "Bearer " + token)
                        .param("type", "EXPENSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(expenseCategoryId))
                .andExpect(jsonPath("$[0].type").value("EXPENSE"));
        mockMvc.perform(delete("/categories/{categoryId}", expenseCategoryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(put("/categories/{categoryId}", categoryId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Rent\",\"type\":\"EXPENSE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rent"))
                .andExpect(jsonPath("$.type").value("EXPENSE"));

        mockMvc.perform(delete("/categories/{categoryId}", categoryId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/categories").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void usersCannotViewOrModifyAnotherUsersCategories() throws Exception {
        String ownerToken = registerAndLogin(uniqueName());
        String otherToken = registerAndLogin(uniqueName());
        String created = mockMvc.perform(post("/categories")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Rent\",\"type\":\"EXPENSE\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String categoryId = JsonPath.read(created, "$.id");

        mockMvc.perform(get("/categories").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(put("/categories/{categoryId}", categoryId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Updated\",\"type\":\"INCOME\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/categories/{categoryId}", categoryId)
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void categoryEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/categories")).andExpect(status().isUnauthorized());
    }
}
