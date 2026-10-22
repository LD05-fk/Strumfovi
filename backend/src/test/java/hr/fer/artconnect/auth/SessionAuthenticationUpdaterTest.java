package hr.fer.artconnect.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.context.SecurityContextRepository;

import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRepository;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.support.TestUsers;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
class SessionAuthenticationUpdaterTest {

    @Mock
    UserRepository userRepository;
    @Mock
    SecurityContextRepository securityContextRepository;
    @Mock
    HttpServletRequest request;
    @Mock
    HttpServletResponse response;

    SessionAuthenticationUpdater updater;

    @BeforeEach
    void setUp() {
        updater = new SessionAuthenticationUpdater(userRepository, securityContextRepository);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anonimniZahtjevDobivaPrincipalIzRedaUsers() {
        // Ovako se prijavljuje POST /dev/login-as (AUTH-01-BE3): prije poziva nema nikakve prijave.
        UUID id = UUID.randomUUID();
        User user = TestUsers.user(id, null, "umjetnik@artconnect.test", UserRole.ARTIST, UserStatus.ACTIVE);
        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        ArtConnectPrincipal principal = updater.update(id, request, response);

        assertThat(principal.getName()).isEqualTo(id.toString());
        assertThat(principal.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ARTIST");
        assertThat(principal.getSubject()).isEqualTo(id.toString());
        assertThat(principal.getEmail()).isEqualTo("umjetnik@artconnect.test");
        assertThat(principal.getIdToken()).isNull();

        var saved = ArgumentCaptor.forClass(SecurityContext.class);
        verify(securityContextRepository).saveContext(saved.capture(), eq(request), eq(response));
        assertThat(saved.getValue().getAuthentication().getPrincipal()).isSameAs(principal);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isSameAs(principal);
    }

    @Test
    void istiKorisnikZadrzavaGooglePodatkeBezUgnjezdivanja() {
        UUID id = UUID.randomUUID();
        DefaultOidcUser google = googleUser("google-sub-1");
        User onboarding = TestUsers.user(id, "google-sub-1", "ana@example.com", null, UserStatus.ONBOARDING);
        loginAs(new ArtConnectPrincipal(onboarding, google));
        // Nakon odabira uloge red u bazi ima ulogu i status ACTIVE.
        User active = TestUsers.user(id, "google-sub-1", "ana@example.com", UserRole.ARTIST, UserStatus.ACTIVE);
        when(userRepository.findById(id)).thenReturn(Optional.of(active));

        ArtConnectPrincipal principal = updater.update(id, request, response);

        assertThat(principal.getRole()).isEqualTo(UserRole.ARTIST);
        assertThat(principal.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(principal.getOidcUser()).isSameAs(google);
        assertThat(principal.getIdToken()).isSameAs(google.getIdToken());
    }

    @Test
    void drugiKorisnikNeNasljedujeGooglePodatkeTrenutneSesije() {
        UUID otherId = UUID.randomUUID();
        loginAs(new ArtConnectPrincipal(
                TestUsers.user(otherId, "google-sub-other", "drugi@example.com", UserRole.ADMIN, UserStatus.ACTIVE),
                googleUser("google-sub-other")));
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.of(
                TestUsers.user(id, null, "novi@artconnect.test", null, UserStatus.ONBOARDING)));

        ArtConnectPrincipal principal = updater.update(id, request, response);

        assertThat(principal.getName()).isEqualTo(id.toString());
        assertThat(principal.getAuthorities()).isEmpty();
        assertThat(principal.getIdToken()).isNull();
        assertThat(principal.getEmail()).isEqualTo("novi@artconnect.test");
    }

    private static DefaultOidcUser googleUser(String sub) {
        Instant now = Instant.now();
        OidcIdToken idToken = OidcIdToken.withTokenValue("google-id-token")
                .subject(sub)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();
        return new DefaultOidcUser(List.of(), idToken);
    }

    private static void loginAs(ArtConnectPrincipal principal) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }
}
