package hr.fer.artconnect.auth;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import hr.fer.artconnect.common.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.annotation.*;
import org.springframework.security.core.authority.*;
import org.springframework.security.core.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.web.bind.annotation.*;

@WebMvcTest(CurrentUserTest.TestController.class)
@Import({SecurityConfig.class, CurrentUserProvider.class, CurrentUserTest.TestController.class})
class CurrentUserTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    CurrentUserProvider provider;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {"principal", "provider"})
    void kontrolerIServisDohvacajuIstogKorisnika(String path) throws Exception {
        var user = new CurrentUser(UUID.randomUUID(), CurrentUser.Role.ARTIST, CurrentUser.Status.ACTIVE);
        var auth = UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
        mvc.perform(get("/api/v1/test/current-user/" + path).with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.id().toString()))
                .andExpect(jsonPath("$.role").value("ARTIST"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void providerOdbijaNedostajuciAnonimniINepodrzaniPrincipal() {
        provjeriNeprijavljenog();
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "test", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));
        provjeriNeprijavljenog();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("other-principal", null, List.of()));
        provjeriNeprijavljenog();
    }

    private void provjeriNeprijavljenog() {
        assertThatThrownBy(provider::getCurrentUser).isInstanceOfSatisfying(ApiException.class,
                error -> assertThat(error.getCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));
    }

    @Test
    void onboardingNemaUloguAFilterJosNeOgranicavaStatus() throws Exception {
        var user = new CurrentUser(UUID.randomUUID(), null, CurrentUser.Status.ONBOARDING);
        assertThat(user.getAuthorities()).isEmpty();
        var auth = UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities());
        mvc.perform(get("/api/v1/test/current-user/provider").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ONBOARDING"));
    }

    @RestController
    public static class TestController {
        private final CurrentUserProvider provider;

        public TestController(CurrentUserProvider provider) {
            this.provider = provider;
        }

        @GetMapping("/api/v1/test/current-user/principal")
        public CurrentUser principal(@AuthenticationPrincipal CurrentUser user) {
            return user;
        }

        @GetMapping("/api/v1/test/current-user/provider")
        public CurrentUser provider() {
            return provider.getCurrentUser();
        }
    }
}
