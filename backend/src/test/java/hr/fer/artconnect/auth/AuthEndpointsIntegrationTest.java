package hr.fer.artconnect.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.support.IntegrationTestBase;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.Session;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;

class AuthEndpointsIntegrationTest extends IntegrationTestBase {
    @Autowired
    JdbcIndexedSessionRepository sessions;
    @Autowired
    JdbcTemplate jdbc;

    @Test
    void csrfMeIOdjavaRadeSaSesijomUBazi() throws Exception {
        var userId = loginAs("ARTIST");
        var jdbcSession = sessions.createSession();
        Session session = jdbcSession;
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext());
        sessions.save(jdbcSession);
        TestSecurityContextHolder.clearContext();
        var sessionCookie = new Cookie("SESSION", Base64.getEncoder().encodeToString(
                session.getId().getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(get("/api/v1/me").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.role").value("ARTIST"));

        jdbc.update("UPDATE users SET role = 'ORGANIZER', organizer_approval = 'PENDING' WHERE id = ?", userId);
        mockMvc.perform(get("/api/v1/me").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ORGANIZER"))
                .andExpect(jsonPath("$.organizerApproval").value("PENDING"));

        var csrfCookie = csrfCookie();
        mockMvc.perform(get("/api/v1/auth/csrf").cookie(csrfCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(csrfCookie.getValue()));

        // Valjan kolačić nije dovoljan: odjava mora provjeriti i poslano zaglavlje.
        mockMvc.perform(post("/api/v1/auth/logout").cookie(sessionCookie, csrfCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        assertThat((Session) sessions.findById(session.getId())).isNotNull();

        var response = mockMvc.perform(post("/api/v1/auth/logout").cookie(sessionCookie, csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andReturn().getResponse();
        assertThat(response.getCookie("SESSION")).isNotNull();
        assertThat(response.getCookie("SESSION").getMaxAge()).isZero();
        assertThat(response.getCookie("XSRF-TOKEN")).isNotNull();
        assertThat(response.getCookie("XSRF-TOKEN").getMaxAge()).isZero();
        assertThat((Session) sessions.findById(session.getId())).isNull();

        mockMvc.perform(get("/api/v1/me").cookie(sessionCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));

        // Odjava briše i CSRF token; za ponovljeni poziv dohvaćamo novi.
        var nextCsrfCookie = csrfCookie();
        mockMvc.perform(post("/api/v1/auth/logout").cookie(sessionCookie, nextCsrfCookie)
                        .header("X-XSRF-TOKEN", nextCsrfCookie.getValue()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    private Cookie csrfCookie() throws Exception {
        var response = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andReturn().getResponse();
        var cookie = response.getCookie("XSRF-TOKEN");
        assertThat(cookie).isNotNull();
        assertThat(response.getContentAsString()).contains(cookie.getValue());
        return cookie;
    }
}
