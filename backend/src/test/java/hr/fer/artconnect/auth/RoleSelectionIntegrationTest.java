package hr.fer.artconnect.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.auth.domain.UserRepository;
import hr.fer.artconnect.support.IntegrationTestBase;
import hr.fer.artconnect.support.TestPrincipals;
import java.util.UUID;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.Cookie;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.Session;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Import(RoleSelectionIntegrationTest.TestController.class)
class RoleSelectionIntegrationTest extends IntegrationTestBase {
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired JdbcIndexedSessionRepository sessions;

    @Test
    void onboardingBlokiraApiADodjelaUlogeOdmahVrijediUIstojSesiji() throws Exception {
        UUID id = createOnboardingUser();
        var jdbcSession = sessions.createSession();
        Session session = jdbcSession;
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(TestPrincipals.authentication(users.findById(id).orElseThrow()));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        sessions.save(jdbcSession);
        var cookie = new Cookie("SESSION", Base64.getEncoder().encodeToString(
                session.getId().getBytes(StandardCharsets.UTF_8)));
        TestSecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/v1/me/organizer-profile").cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ONBOARDING_REQUIRED"));
        mockMvc.perform(get("/api/v1/art-fields").cookie(cookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ONBOARDING_REQUIRED"));
        mockMvc.perform(get("/api/v1/art-fields")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/me").cookie(cookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ONBOARDING"));
        var csrfCookie = mockMvc.perform(get("/api/v1/auth/csrf").cookie(cookie))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrfCookie).isNotNull();
        for (String body : new String[] {"", "{\"role\":\"ADMIN\"}"}) {
            mockMvc.perform(post("/api/v1/me/role").cookie(cookie, csrfCookie)
                            .header("X-XSRF-TOKEN", csrfCookie.getValue())
                            .contentType("application/json").content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
        mockMvc.perform(post("/api/v1/me/role").cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType("application/json").content("{\"role\":\"ARTIST\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/me/role").cookie(cookie, csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType("application/json").content("{\"role\":\"ORGANIZER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizerApproval").value("APPROVED"));

        // Bez testne autentikacije: ovlast se mora učitati iz iste JDBC sesije.
        TestSecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/test/selected-role").cookie(cookie))
                .andExpect(status().isOk()).andExpect(content().string("ORGANIZER"));
        mockMvc.perform(get("/api/v1/me").cookie(cookie))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE"));
        jdbc.update("UPDATE users SET role = NULL, status = 'ONBOARDING', organizer_approval = NULL WHERE id = ?", id);
        mockMvc.perform(post("/api/v1/auth/logout").cookie(cookie, csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isNoContent());
    }

    @RestController
    static class TestController {
        @GetMapping("/api/v1/test/selected-role")
        @PreAuthorize("hasRole('ORGANIZER')")
        public String organizer() {
            return "ORGANIZER";
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ARTIST", "ORGANIZER"})
    void odabirSpremaPodatkeIProfilADrugiPozivVraca409(String role) throws Exception {
        UUID id = createOnboardingUser();
        var auth = TestPrincipals.authentication(users.findById(id).orElseThrow());
        var csrfCookie = csrfCookie();

        mockMvc.perform(post("/api/v1/me/role").with(authentication(auth)).cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType("application/json").content("{\"role\":\"" + role + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(role))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.organizerApproval").value(
                        "ORGANIZER".equals(role) ? "APPROVED" : null));

        var user = users.findById(id).orElseThrow();
        assertThat(user.getRole().name()).isEqualTo(role);
        assertThat(user.getStatus().name()).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM organizer_profiles WHERE user_id = ?",
                Integer.class, id)).isEqualTo("ORGANIZER".equals(role) ? 1 : 0);
        if ("ORGANIZER".equals(role)) {
            assertThat(jdbc.queryForObject("SELECT organization_name FROM organizer_profiles WHERE user_id = ?",
                    String.class, id)).isNull();
        }

        mockMvc.perform(post("/api/v1/me/role").with(authentication(auth)).cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType("application/json").content("{\"role\":\"ARTIST\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ROLE_ALREADY_CHOSEN"));
        assertThat(users.findById(id).orElseThrow().getRole().name()).isEqualTo(role);
    }

    @Test
    void dvaIstodobnaOdabiraStvarajuSamoJedanProfil() throws Exception {
        UUID id = createOnboardingUser();
        var auth = TestPrincipals.authentication(users.findById(id).orElseThrow());
        var csrfCookie = csrfCookie();
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> chooseOrganizer(auth, start, csrfCookie));
            var second = executor.submit(() -> chooseOrganizer(auth, start, csrfCookie));
            start.countDown();
            assertThat(new Integer[] {first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS)})
                    .containsExactlyInAnyOrder(200, 409);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM organizer_profiles WHERE user_id = ?",
                Integer.class, id)).isEqualTo(1);
    }

    private int chooseOrganizer(Authentication auth, CountDownLatch start, Cookie csrfCookie) throws Exception {
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Test nije pokrenuo istodobne zahtjeve.");
        }
        return mockMvc.perform(post("/api/v1/me/role").with(authentication(auth)).cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .contentType("application/json").content("{\"role\":\"ORGANIZER\"}"))
                .andReturn().getResponse().getStatus();
    }

    private UUID createOnboardingUser() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, email, display_name, status) VALUES (?, ?, ?, 'ONBOARDING')",
                id, id + "@artconnect.test", "Novi korisnik");
        return id;
    }

    private Cookie csrfCookie() throws Exception {
        var cookie = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        return cookie;
    }
}
