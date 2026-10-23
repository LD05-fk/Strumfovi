package hr.fer.artconnect.auth;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.auth.domain.OrganizerApproval;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRepository;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.auth.service.RoleSelectionService;
import hr.fer.artconnect.auth.web.AuthController;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import hr.fer.artconnect.support.TestPrincipals;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class, properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-client",
        "spring.security.oauth2.client.registration.google.client-secret=test-secret"
})
@Import({SecurityConfig.class, CurrentUserProvider.class})
class AuthControllerTest {
    @Autowired
    MockMvc mvc;
    @MockitoBean
    ArtConnectOidcUserService oidcUserService;
    @MockitoBean
    UserRepository userRepository;
    @MockitoBean
    RoleSelectionService roleSelectionService;

    @Test
    void odabirUlogeVracaAktualnogKorisnika() throws Exception {
        for (var role : new UserRole[] {UserRole.ARTIST, UserRole.ORGANIZER}) {
            var user = user(role, UserStatus.ACTIVE);
            when(roleSelectionService.chooseRole(role)).thenReturn(user);
            mvc.perform(post("/api/v1/me/role")
                            .with(authentication(TestPrincipals.authentication(user)))
                            .with(csrf())
                            .contentType("application/json").content("{\"role\":\"" + role + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value(role.name()))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }
    }

    @Test
    void neispravnaUlogaIliTijeloVraca400() throws Exception {
        var user = user(null, UserStatus.ONBOARDING);
        for (String body : new String[] {"", "{}", "null", "{\"role\":null}", "{\"role\":\"ADMIN\"}",
                "{\"role\":\"UNKNOWN\"}", "{\"role\":\"\"}"}) {
            mvc.perform(post("/api/v1/me/role")
                            .with(authentication(TestPrincipals.authentication(user)))
                            .with(csrf())
                            .contentType("application/json").content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
        verifyNoInteractions(roleSelectionService);
    }

    @Test
    void odabirBezSesijeVraca401ABezCsrfVraca403() throws Exception {
        mvc.perform(post("/api/v1/me/role")
                        .with(csrf())
                        .contentType("application/json").content("{\"role\":\"ARTIST\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        var user = user(null, UserStatus.ONBOARDING);
        mvc.perform(post("/api/v1/me/role")
                        .with(authentication(TestPrincipals.authentication(user)))
                        .contentType("application/json").content("{\"role\":\"ARTIST\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ponovljeniOdabirVraca409() throws Exception {
        var user = user(UserRole.ARTIST, UserStatus.ACTIVE);
        when(roleSelectionService.chooseRole(UserRole.ARTIST)).thenThrow(new ApiException(
                ErrorCode.ROLE_ALREADY_CHOSEN, "Uloga je već odabrana."));
        mvc.perform(post("/api/v1/me/role")
                        .with(authentication(TestPrincipals.authentication(user)))
                        .with(csrf())
                        .contentType("application/json").content("{\"role\":\"ARTIST\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ROLE_ALREADY_CHOSEN"));
    }

    @Test
    void meVracaAktualnePodatkeIzBaze() throws Exception {
        var user = user(UserRole.ARTIST, UserStatus.ACTIVE);
        var sessionAuthentication = TestPrincipals.authentication(user);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        mvc.perform(get("/api/v1/me").with(authentication(sessionAuthentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ARTIST"));

        // Uloga u principalu ostaje ARTIST, ali je administrator promijenio red u bazi.
        when(user.getRole()).thenReturn(UserRole.ORGANIZER);
        when(user.getOrganizerApproval()).thenReturn(OrganizerApproval.PENDING);
        when(user.getDisplayName()).thenReturn("Novo ime");
        when(user.getAvatarUrl()).thenReturn("https://example.com/avatar.png");

        mvc.perform(get("/api/v1/me").with(authentication(sessionAuthentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.displayName").value("Novo ime"))
                .andExpect(jsonPath("$.email").value("korisnik@example.com"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/avatar.png"))
                .andExpect(jsonPath("$.role").value("ORGANIZER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.organizerApproval").value("PENDING"));
    }

    @Test
    void meJeDostupanTijekomOnboardingaBezUloge() throws Exception {
        var user = user(null, UserStatus.ONBOARDING);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        mvc.perform(get("/api/v1/me").with(authentication(TestPrincipals.authentication(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(nullValue()))
                .andExpect(jsonPath("$.status").value("ONBOARDING"))
                .andExpect(jsonPath("$.avatarUrl").value(nullValue()))
                .andExpect(jsonPath("$.organizerApproval").value(nullValue()));
    }

    @Test
    void meBezSesijeVraca401() throws Exception {
        mvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void meVraca401KadKorisnikIzSesijeViseNePostojiUBazi() throws Exception {
        var user = user(UserRole.ARTIST, UserStatus.ACTIVE);
        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/me").with(authentication(TestPrincipals.authentication(user))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    private User user(UserRole role, UserStatus status) {
        var user = mock(User.class);
        when(user.getId()).thenReturn(UUID.randomUUID());
        when(user.getRole()).thenReturn(role);
        when(user.getStatus()).thenReturn(status);
        when(user.getDisplayName()).thenReturn("Korisnik");
        when(user.getEmail()).thenReturn("korisnik@example.com");
        return user;
    }
}
