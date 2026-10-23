package hr.fer.artconnect.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import hr.fer.artconnect.support.IntegrationTestBase;
import jakarta.servlet.http.Cookie;

@Import(DevLoginIntegrationTest.TestController.class)
@ActiveProfiles("dev")
class DevLoginIntegrationTest extends IntegrationTestBase {
    private static final String LOGIN_PATH = "/api/v1/dev/login-as";

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void prijavaPostavljaPrincipalUSesijuINormaliziraEmail() throws Exception {
        UUID userId = addUser("umjetnik@artconnect.test", "ARTIST", "ACTIVE");
        Cookie oldSession = createSession();

        MvcResult result = mockMvc.perform(post(LOGIN_PATH)
                        .cookie(oldSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"UMJETNIK@ARTCONNECT.TEST"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.displayName").value("Test korisnik"))
                .andExpect(jsonPath("$.email").value("umjetnik@artconnect.test"))
                .andExpect(jsonPath("$.role").value("ARTIST"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();

        Cookie newSession = result.getResponse().getCookie("SESSION");
        assertThat(newSession).isNotNull();
        assertThat(newSession.getValue()).isNotEqualTo(oldSession.getValue());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT last_login_at IS NOT NULL FROM users WHERE id = ?", Boolean.class, userId))
                .isTrue();

        mockMvc.perform(get("/api/v1/dev/test/current-user").cookie(newSession))
                .andExpect(status().isOk())
                .andExpect(content().string(userId.toString()));
    }

    @Test
    void prijavaAktiviraPozvanogKorisnikaBezGoogleSuba() throws Exception {
        UUID userId = addUser("pozvani@artconnect.test", "ADMIN", "INVITED");

        mockMvc.perform(post(LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"pozvani@artconnect.test"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.role").value("ADMIN"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM users WHERE id = ?", String.class, userId)).isEqualTo("ACTIVE");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT google_sub FROM users WHERE id = ?", String.class, userId)).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT last_login_at IS NOT NULL FROM users WHERE id = ?", Boolean.class, userId))
                .isTrue();
    }

    @Test
    void neispravanEmailVracaValidacijskuGresku() throws Exception {
        for (String body : new String[] {
                "{}",
                """
                {"email":""}
                """,
                """
                {"email":"nije-email"}
                """,
                "{\"email\":\"" + "a".repeat(243) + "@example.com\"}"
        }) {
            mockMvc.perform(post(LOGIN_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
    }

    @Test
    void nepostojeciIObrisaniKorisnikVracajuNotFound() throws Exception {
        UUID deletedId = addUser("obrisani@artconnect.test", "ARTIST", "DELETED");

        for (String email : new String[] {"nepostojeci@artconnect.test", "obrisani@artconnect.test"}) {
            mockMvc.perform(post(LOGIN_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"" + email + "\"}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM users WHERE id = ?", String.class, deletedId)).isEqualTo("DELETED");
        String emailAtLimit = "a".repeat(64) + "@"
                + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(61);
        assertThat(emailAtLimit).hasSize(254);
        mockMvc.perform(post(LOGIN_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + emailAtLimit + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    private UUID addUser(String email, String role, String status) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, display_name, role, status, organizer_approval) "
                        + "VALUES (?, ?, 'Test korisnik', ?, ?, ?)",
                id, email, role, status, "ORGANIZER".equals(role) ? "APPROVED" : null);
        return id;
    }

    private Cookie createSession() throws Exception {
        return mockMvc.perform(get("/api/v1/dev/test/session"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookie("SESSION");
    }

    @RestController
    public static class TestController {
        private final CurrentUserProvider currentUserProvider;

        public TestController(CurrentUserProvider currentUserProvider) {
            this.currentUserProvider = currentUserProvider;
        }

        @GetMapping("/api/v1/dev/test/session")
        public String createSession(jakarta.servlet.http.HttpSession session) {
            session.setAttribute("test", "value");
            return "OK";
        }

        @GetMapping("/api/v1/dev/test/current-user")
        public String currentUser() {
            return currentUserProvider.getCurrentUser().getId().toString();
        }
    }
}
