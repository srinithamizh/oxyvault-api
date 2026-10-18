package io.github.srinithamizh.oxyvault;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests of registration, login and logout against an in-memory H2 database.
 * <p>
 * The database is shared by every test in the run, so each test uses its own unique username.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    private static final String PASSWORD = "Str0ng!Pass";

    @Autowired
    private MockMvc mockMvc;

    private static String uniqueName() {
        return "user" + UUID.randomUUID().toString().substring(0, 8);
    }

    private ResultActions register(String username, String password) throws Exception {
        String body = """
                {"username":"%s","email":"%s@example.com","password":"%s"}
                """.formatted(username, username, password);

        return mockMvc.perform(post("/auth/register").contentType(APPLICATION_JSON).content(body));
    }

    private ResultActions attemptLogin(String identifier, String password) throws Exception {
        String body = """
                {"identifier":"%s","password":"%s"}
                """.formatted(identifier, password);

        return mockMvc.perform(post("/auth/login").contentType(APPLICATION_JSON).content(body));
    }

    private String loginAndGetToken(String identifier, String password) throws Exception {
        String response = attemptLogin(identifier, password)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return JsonPath.read(response, "$.token");
    }

    // Regression: tokenVersion used to be unique, and the builder inserted null, so registration failed.
    @Test
    void twoUsersCanRegister() throws Exception {
        register(uniqueName(), PASSWORD).andExpect(status().isCreated());
        register(uniqueName(), PASSWORD).andExpect(status().isCreated());
    }

    @Test
    void duplicateRegistrationIsConflict() throws Exception {
        String name = uniqueName();

        register(name, PASSWORD).andExpect(status().isCreated());
        register(name, PASSWORD).andExpect(status().isConflict());
    }

    // Regression: validation errors used to come back as 500.
    @Test
    void invalidRegistrationIsBadRequestWithFieldErrors() throws Exception {
        register("ab", "weak")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void loginWithUsernameReturnsToken() throws Exception {
        String name = uniqueName();
        register(name, PASSWORD).andExpect(status().isCreated());

        attemptLogin(name, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    // Regression: emails are stored lower-cased, but login used to compare the identifier as typed.
    @Test
    void loginWithEmailIsCaseInsensitive() throws Exception {
        String name = uniqueName();
        register(name, PASSWORD).andExpect(status().isCreated());

        attemptLogin((name + "@example.com").toUpperCase(), PASSWORD)
                .andExpect(status().isOk());
    }

    // Regression: bad credentials used to come back as 500.
    @Test
    void wrongPasswordIsUnauthorized() throws Exception {
        String name = uniqueName();
        register(name, PASSWORD).andExpect(status().isCreated());

        attemptLogin(name, "Wrong!Pass1").andExpect(status().isUnauthorized());
    }

    @Test
    void unknownUserIsUnauthorized() throws Exception {
        attemptLogin(uniqueName(), PASSWORD).andExpect(status().isUnauthorized());
    }

    // Regression: a stale token in the header used to make public endpoints return 401.
    @Test
    void staleTokenDoesNotBlockLogin() throws Exception {
        String name = uniqueName();
        register(name, PASSWORD).andExpect(status().isCreated());

        mockMvc.perform(post("/auth/login")
                        .header("Authorization", "Bearer not-a-valid-token")
                        .contentType(APPLICATION_JSON)
                        .content("{\"identifier\":\"" + name + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk());
    }

    // Regression: unauthenticated requests used to get 403 instead of 401.
    @Test
    void protectedEndpointWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(post("/auth/logout")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidTokenOnProtectedEndpointIsUnauthorized() throws Exception {
        mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer not-a-valid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesToken() throws Exception {
        String name = uniqueName();
        register(name, PASSWORD).andExpect(status().isCreated());
        String token = loginAndGetToken(name, PASSWORD);

        mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // The same token must no longer be accepted.
        mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void newLoginAfterLogoutGivesWorkingToken() throws Exception {
        String name = uniqueName();
        register(name, PASSWORD).andExpect(status().isCreated());
        String oldToken = loginAndGetToken(name, PASSWORD);

        mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer " + oldToken))
                .andExpect(status().isOk());

        String newToken = loginAndGetToken(name, PASSWORD);

        mockMvc.perform(post("/auth/logout").header("Authorization", "Bearer " + newToken))
                .andExpect(status().isOk());
    }
}
