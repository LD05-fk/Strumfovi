package hr.fer.artconnect.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRepository;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import hr.fer.artconnect.support.TestPrincipals;

@WebMvcTest(CurrentUserTest.TestController.class)
@Import({SecurityConfig.class, CurrentUserProvider.class, CurrentUserTest.TestController.class})
class CurrentUserTest {
    @Autowired
    MockMvc mvc;
    @Autowired
    CurrentUserProvider provider;
    @MockitoBean
    ArtConnectOidcUserService oidcUserService;
    @MockitoBean
    UserRepository userRepository;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {"principal", "provider"})
    void kontrolerIServisDohvacajuIstogKorisnika(String path) throws Exception {
        var user = user(UserRole.ARTIST, UserStatus.ACTIVE);
        mvc.perform(get("/api/v1/test/current-user/" + path).with(authentication(TestPrincipals.authentication(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
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

    @Test
    void providerOdbijaPrincipalaBezRedaUBazi() {
        var user = mock(User.class);
        when(user.getId()).thenReturn(UUID.randomUUID());
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(user.getRole()).thenReturn(UserRole.ARTIST);
        // Korisnik je u sesiji, ali u bazi ga više nema.
        SecurityContextHolder.getContext().setAuthentication(TestPrincipals.authentication(user));
        provjeriNeprijavljenog();
    }

    private void provjeriNeprijavljenog() {
        assertThatThrownBy(provider::getCurrentUser).isInstanceOfSatisfying(ApiException.class,
                error -> assertThat(error.getCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));
    }

    @Test
    void onboardingNemaUloguIFilterOgranicavaPristup() throws Exception {
        var user = user(null, UserStatus.ONBOARDING);
        var auth = TestPrincipals.authentication(user);
        assertThat(auth.getAuthorities()).isEmpty();
        mvc.perform(get("/api/v1/test/current-user/provider").with(authentication(auth)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ONBOARDING_REQUIRED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/%61pi/v1/test/current-user/provider", "/api/%761/test/current-user/provider"})
    void kodiranaPutanjaKoristiIstaPravilaFiltraIKontrolera(String path) throws Exception {
        var user = user(null, UserStatus.ONBOARDING);
        var auth = TestPrincipals.authentication(user);
        // URI sprječava da MockMvc ponovno kodira znak % iz testne putanje.
        mvc.perform(get(java.net.URI.create(path)).with(authentication(auth)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ONBOARDING_REQUIRED"));
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        mvc.perform(get(java.net.URI.create(path)).with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()));
    }

    private User user(UserRole role, UserStatus status) {
        var user = mock(User.class);
        var id = UUID.randomUUID();
        when(user.getId()).thenReturn(id);
        when(user.getRole()).thenReturn(role);
        when(user.getStatus()).thenReturn(status);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        return user;
    }

    record CurrentUserResponse(UUID id, UserRole role, UserStatus status) {
    }

    @RestController
    public static class TestController {
        private final CurrentUserProvider provider;

        public TestController(CurrentUserProvider provider) {
            this.provider = provider;
        }

        @GetMapping("/api/v1/test/current-user/principal")
        public CurrentUserResponse principal(@AuthenticationPrincipal ArtConnectPrincipal principal) {
            return new CurrentUserResponse(principal.getUserId(), principal.getRole(), principal.getStatus());
        }

        @GetMapping("/api/v1/test/current-user/provider")
        public CurrentUserResponse provider() {
            var user = provider.getCurrentUser();
            return new CurrentUserResponse(user.getId(), user.getRole(), user.getStatus());
        }
    }
}
