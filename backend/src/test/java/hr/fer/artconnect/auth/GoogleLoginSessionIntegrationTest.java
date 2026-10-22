package hr.fer.artconnect.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRepository;
import hr.fer.artconnect.support.IntegrationTestBase;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Prijava nakon Googlea: ArtConnectPrincipal se serijalizira u spring_session i indeksira po users.id.
@Import(GoogleLoginSessionIntegrationTest.TestController.class)
class GoogleLoginSessionIntegrationTest extends IntegrationTestBase {

    @Autowired
    UserRepository users;
    @Autowired
    FindByIndexNameSessionRepository<? extends Session> sessions;

    @Test
    void principalSeSpremaUSesijuIIndeksiraPoIdKorisnika() throws Exception {
        UUID userId = users.save(User.createOnboardingUser(
                "google-sub-login-test", "prijava@artconnect.test", "Prijava", null)).getId();

        // oidcLogin() glumi Googleov OidcUser, a updater ga zamjenjuje principalom iz baze.
        var response = mockMvc.perform(post("/api/v1/test/google-login/" + userId)
                        .with(oidcLogin())
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string(userId.toString()))
                .andReturn().getResponse();
        Cookie sessionCookie = response.getCookie("SESSION");
        assertThat(sessionCookie).isNotNull();

        // Sesija se ponovno čita iz baze, pa je principal prošao serijalizaciju.
        var indexed = sessions.findByPrincipalName(userId.toString());
        assertThat(indexed).hasSize(1);
        SecurityContext context = indexed.values().iterator().next()
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(context.getAuthentication().getPrincipal())
                .isInstanceOfSatisfying(ArtConnectPrincipal.class,
                        principal -> assertThat(principal.getName()).isEqualTo(userId.toString()));

        // Drugi zahtjev nosi samo kolačić; korisnika daje CurrentUserProvider iz baze.
        TestSecurityContextHolder.clearContext();
        SecurityContextHolder.clearContext();
        mockMvc.perform(get("/api/v1/test/google-login/current").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(content().string(userId.toString()));
    }

    @RestController
    public static class TestController {
        private final SessionAuthenticationUpdater updater;
        private final CurrentUserProvider currentUserProvider;

        public TestController(SessionAuthenticationUpdater updater, CurrentUserProvider currentUserProvider) {
            this.updater = updater;
            this.currentUserProvider = currentUserProvider;
        }

        @PostMapping("/api/v1/test/google-login/{userId}")
        public String login(@PathVariable UUID userId, HttpServletRequest request, HttpServletResponse response) {
            return updater.update(userId, request, response).getName();
        }

        @GetMapping("/api/v1/test/google-login/current")
        public String current() {
            return currentUserProvider.getCurrentUser().getId().toString();
        }
    }
}
