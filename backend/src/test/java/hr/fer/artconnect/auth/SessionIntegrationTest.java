package hr.fer.artconnect.auth;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.servlet.http.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.*;
import org.springframework.boot.testcontainers.service.connection.*;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.web.context.*;
import org.springframework.session.*;
import org.springframework.session.jdbc.*;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.web.bind.annotation.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.*;

// Pokrenuti web poslužitelj potreban je da Boot primijeni postavke sesijskog kolačića.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(SessionIntegrationTest.TestController.class)
// I uz oba profila produkcija mora zadržati CSRF zaštitu razvojnih ruta.
@ActiveProfiles({"dev", "prod"})
@Testcontainers
class SessionIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    JdbcIndexedSessionRepository sessions;
    @Autowired
    FindByIndexNameSessionRepository<?> indexedSessions;
    @Autowired
    MockMvc mvc;

    @Test
    void prijavaIzBazeDostupnaJePrekoSessionKolacica() throws Exception {
        var jdbcSession = sessions.createSession();
        // JDBC klasa je interna pa atributima pristupamo preko javnog Session sučelja.
        Session session = jdbcSession;
        var user = User.withUsername("session-test").password("unused").roles("ARTIST").build();
        var authentication = UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(authentication));
        sessions.save(jdbcSession);

        try {
            // Ponovno čitanje iz baze i novi HTTP zahtjev ne ovise o izvornom objektu sesije.
            Session reloaded = sessions.findById(session.getId());
            assertThat(reloaded).isNotNull();
            assertThat(reloaded.getMaxInactiveInterval()).isEqualTo(Duration.ofHours(8));
            assertThat(indexedSessions.findByPrincipalName("session-test")).containsKey(session.getId());

            String cookieValue = Base64.getEncoder().encodeToString(
                    session.getId().getBytes(StandardCharsets.UTF_8));
            mvc.perform(get("/api/v1/auth/session-test").cookie(new Cookie("SESSION", cookieValue)))
                    .andExpect(status().isOk());
        } finally {
            sessions.deleteById(session.getId());
        }
    }

    @Test
    void produkcijskiKolaciciImajuPotrebneZastite() throws Exception {
        var response = mvc.perform(get("/api/v1/artists/session-test").secure(true))
                .andExpect(status().isOk()).andReturn().getResponse();
        var cookie = response.getCookie("SESSION");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getAttribute("SameSite")).isEqualTo("Lax");

        var csrfResponse = mvc.perform(get("/api/v1/auth/csrf").secure(true))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertThat(csrfResponse.getCookie("XSRF-TOKEN").getSecure()).isTrue();
    }

    @Test
    void produkcijaNeDopustaDevIzuzeceCsrf() throws Exception {
        mvc.perform(post("/api/v1/dev/login-as"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @RestController
    public static class TestController {
        @GetMapping("/api/v1/artists/session-test")
        public String createSession(HttpSession session) {
            session.setAttribute("test", "value");
            return "OK";
        }

        @GetMapping("/api/v1/auth/session-test")
        public String protectedEndpoint() {
            return "OK";
        }
    }
}
