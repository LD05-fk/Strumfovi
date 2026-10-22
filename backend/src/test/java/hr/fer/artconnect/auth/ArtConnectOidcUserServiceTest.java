
package hr.fer.artconnect.auth;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRepository;
import hr.fer.artconnect.auth.domain.UserStatus;

@ExtendWith(MockitoExtension.class)
class ArtConnectOidcUserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    OidcUser oidcUser;

    ArtConnectOidcUserService oidcUserService;

    @BeforeEach
    void setUp() {
        oidcUserService = new ArtConnectOidcUserService(userRepository);
    }

    @Test
    void blokiraniKorisnikNeMozeSePrijaviti() {
        User user = mock(User.class);
        when(user.getStatus()).thenReturn(UserStatus.BLOCKED);
        when(oidcUser.getEmailVerified()).thenReturn(true);
        when(oidcUser.getEmail()).thenReturn("blokiran@example.com");
        when(oidcUser.getSubject()).thenReturn("google-sub-123");

        when(userRepository.findByGoogleSub("google-sub-123"))
                .thenReturn(Optional.of(user));
        assertEquals(UserStatus.BLOCKED, user.getStatus());
        
        OAuth2AuthenticationException exception = assertThrows(
                OAuth2AuthenticationException.class,
                () -> oidcUserService.processOidcUser(oidcUser));
        verify(userRepository).findByGoogleSub("google-sub-123");
        assertEquals("account_disabled", exception.getError().getErrorCode());

        // Profil blokiranog korisnika ne smije se ažurirati.
        verify(user, never()).updateGoogleProfile(anyString(), any(), any());
    }

    @Test
    void obrisaniKorisnikNeMozeSePrijaviti() {
        User user = mock(User.class);
        when(user.getStatus()).thenReturn(UserStatus.DELETED);
        when(oidcUser.getEmailVerified()).thenReturn(true);
        when(oidcUser.getEmail()).thenReturn("obrisan@example.com");
        when(oidcUser.getSubject()).thenReturn("google-sub-456");

        when(userRepository.findByGoogleSub("google-sub-456"))
                .thenReturn(Optional.of(user));
        assertEquals(UserStatus.DELETED, user.getStatus());
        
        OAuth2AuthenticationException exception = assertThrows(
                OAuth2AuthenticationException.class,
                () -> oidcUserService.processOidcUser(oidcUser));
        verify(userRepository).findByGoogleSub("google-sub-456");
        assertEquals("account_deleted", exception.getError().getErrorCode());

        verify(user, never()).updateGoogleProfile(anyString(), any(), any());
    }
    @Test
    void nepotvrdeniEmailOdbijaPrijavu() {
        when(oidcUser.getEmailVerified()).thenReturn(false);

        OAuth2AuthenticationException exception = assertThrows(
                OAuth2AuthenticationException.class,
                () -> oidcUserService.processOidcUser(oidcUser));

        assertEquals(
                "email_not_verified",
                exception.getError().getErrorCode()
        );

        verify(userRepository, never()).findByGoogleSub(anyString());
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
    }
    @Test
    void postojeciAktivniKorisnikMozeSePrijaviti(){
        User user = mock(User.class); 
        UUID userId = UUID.randomUUID();
        when(user.getId()).thenReturn(userId);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(oidcUser.getEmailVerified()).thenReturn(true);
        when(oidcUser.getEmail()).thenReturn("ana@example.com");
        when(oidcUser.getSubject()).thenReturn("google-sub-789");
        when(oidcUser.getFullName()).thenReturn("Ana");
        when(oidcUser.getPicture()).thenReturn("https://example.com/ana.png");
        when(userRepository.findByGoogleSub("google-sub-789")).thenReturn(Optional.of(user));

        ArtConnectPrincipal result = oidcUserService.processOidcUser(oidcUser);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());

        verify(user).updateGoogleProfile(
            "ana@example.com",
            "Ana",
            "https://example.com/ana.png");

        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void pozvaniKorisnikSeAktiviraPrvomGooglePrijavom() {
        User user = mock(User.class); 
        UUID userId = UUID.randomUUID();
        when(user.getId()).thenReturn(userId);
        when(user.getStatus()).thenReturn(UserStatus.INVITED);
        when(oidcUser.getEmailVerified()).thenReturn(true);
        when(oidcUser.getEmail()).thenReturn("ana@example.com");
        when(oidcUser.getSubject()).thenReturn("google-sub-789");
        when(oidcUser.getFullName()).thenReturn("Ana");
        when(oidcUser.getPicture()).thenReturn("https://example.com/ana.png");
        when(userRepository.findByGoogleSub("google-sub-789")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.of(user));

        ArtConnectPrincipal result = oidcUserService.processOidcUser(oidcUser);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());

        verify(user).activateInvitedGoogleAccount("google-sub-789", "ana@example.com", "Ana", "https://example.com/ana.png");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void noviGoogleKorisnikDobivaOnboardingStatus() {
        when(oidcUser.getEmailVerified()).thenReturn(true);
        when(oidcUser.getEmail()).thenReturn("ana@example.com");
        when(oidcUser.getSubject()).thenReturn("google-sub-new");
        when(oidcUser.getFullName()).thenReturn("Ana");
        when(oidcUser.getPicture()).thenReturn("https://example.com/ana.png");

        when(userRepository.findByGoogleSub("google-sub-new")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.empty());

        User savedUser = mock(User.class);
        UUID savedUserId = UUID.randomUUID();
        when(savedUser.getId()).thenReturn(savedUserId);
        when(savedUser.getStatus()).thenReturn(UserStatus.ONBOARDING);
        when(savedUser.getRole()).thenReturn(null);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        ArtConnectPrincipal result = oidcUserService.processOidcUser(oidcUser);

        assertNotNull(result);
        assertEquals(savedUserId, result.getUserId());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        verify(userRepository).save(captor.capture());

        User newUser = captor.getValue();

        assertEquals("google-sub-new", newUser.getGoogleSub());
        assertEquals("ana@example.com", newUser.getEmail());
        assertEquals("Ana", newUser.getDisplayName());
        assertEquals("https://example.com/ana.png", newUser.getAvatarUrl());
        assertEquals(UserStatus.ONBOARDING, newUser.getStatus());
        assertEquals(null, newUser.getRole());
        assertEquals(null, newUser.getOrganizerApproval());
    }

    @Test
    void postojeciEmailKorisnikaKojiNijePozvanOdbijaPrijavu() {
        User user = mock(User.class);

        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);

        when(oidcUser.getEmailVerified()).thenReturn(true);
        when(oidcUser.getEmail()).thenReturn("ana@example.com");
        when(oidcUser.getSubject()).thenReturn("google-sub-new");

        when(userRepository.findByGoogleSub("google-sub-new"))
                .thenReturn(Optional.empty());

        when(userRepository.findByEmailIgnoreCase("ana@example.com"))
                .thenReturn(Optional.of(user));

        OAuth2AuthenticationException exception = assertThrows(
                OAuth2AuthenticationException.class,
                () -> oidcUserService.processOidcUser(oidcUser)
        );

        assertEquals(
                "account_conflict",
                exception.getError().getErrorCode()
        );

        verify(user, never()).activateInvitedGoogleAccount(
                anyString(), anyString(), anyString(), any()
        );
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void nedostajuciEmailOdbijaPrijavu() {
        when(oidcUser.getEmailVerified()).thenReturn(true);
        when(oidcUser.getEmail()).thenReturn(null);

        OAuth2AuthenticationException exception = assertThrows(OAuth2AuthenticationException.class, () -> oidcUserService.processOidcUser(oidcUser));

        assertEquals("email_missing", exception.getError().getErrorCode());

        verify(userRepository, never()).findByGoogleSub(anyString());
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());
    }

    @Test
    void akoImeNijeDostupnoEmailSeKoristiKaoIme() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(UUID.randomUUID());
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);

        when(oidcUser.getEmailVerified()).thenReturn(true);
        when(oidcUser.getEmail()).thenReturn("ana@example.com");
        when(oidcUser.getSubject()).thenReturn("google-sub-789");
        when(oidcUser.getFullName()).thenReturn(" ");
        when(oidcUser.getPicture()).thenReturn(null);

        when(userRepository.findByGoogleSub("google-sub-789")).thenReturn(Optional.of(user));

        ArtConnectPrincipal result = oidcUserService.processOidcUser(oidcUser);

        assertNotNull(result);

        verify(user).updateGoogleProfile("ana@example.com", "ana@example.com", null);
    }

    @Test
    void predugoImeSkracujeSeNa120Znakova() {
        User user = mock(User.class);
        String longName = "A".repeat(150);
        when(user.getId()).thenReturn(UUID.randomUUID());
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);

        when(oidcUser.getEmailVerified()).thenReturn(true);
        when(oidcUser.getEmail()).thenReturn("ana@example.com");
        when(oidcUser.getSubject()).thenReturn("google-sub-789");
        when(oidcUser.getFullName()).thenReturn(longName);
        when(oidcUser.getPicture()).thenReturn(null);

        when(userRepository.findByGoogleSub("google-sub-789")).thenReturn(Optional.of(user));

        ArtConnectPrincipal result = oidcUserService.processOidcUser(oidcUser);

        assertNotNull(result);

        verify(user).updateGoogleProfile("ana@example.com", "A".repeat(120), null);
    }

    @Test
    void emailSeNormaliziraUMalaSlova() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(UUID.randomUUID());
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);

        when(oidcUser.getEmailVerified()).thenReturn(true);
        when(oidcUser.getEmail()).thenReturn("Ana.Primjer@Example.COM");
        when(oidcUser.getSubject()).thenReturn("google-sub-789");
        when(oidcUser.getFullName()).thenReturn("Ana");
        when(oidcUser.getPicture()).thenReturn(null);

        when(userRepository.findByGoogleSub("google-sub-789")).thenReturn(Optional.of(user));

        ArtConnectPrincipal result = oidcUserService.processOidcUser(oidcUser);

        assertNotNull(result);

        verify(userRepository).findByGoogleSub("google-sub-789");
        verify(userRepository, never()).findByEmailIgnoreCase(anyString());

        verify(user).updateGoogleProfile("ana.primjer@example.com", "Ana", null);
    }
    @Test
    void obrisaniKorisnikBezGoogleSubIEmailaDobivaNoviOnboardingRacun() {
        when(oidcUser.getEmailVerified()).thenReturn(true);
        when(oidcUser.getEmail()).thenReturn("obrisan@example.com");
        when(oidcUser.getSubject()).thenReturn("google-sub-new");
        when(oidcUser.getFullName()).thenReturn("Novi korisnik");
        when(oidcUser.getPicture()).thenReturn(null);

        // Stari račun više se ne može pronaći po Google sub-u
        // jer je pri brisanju googleSub postavljen na null.
        when(userRepository.findByGoogleSub("google-sub-new"))
                .thenReturn(Optional.empty());

        // Stari račun više se ne može pronaći po e-mailu
        // jer je pri brisanju email postavljen na null.
        when(userRepository.findByEmailIgnoreCase("obrisan@example.com"))
                .thenReturn(Optional.empty());

        User savedUser = mock(User.class);
        UUID newUserId = UUID.randomUUID();

        when(savedUser.getId()).thenReturn(newUserId);
        when(savedUser.getStatus()).thenReturn(UserStatus.ONBOARDING);
        when(savedUser.getRole()).thenReturn(null);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        ArtConnectPrincipal result = oidcUserService.processOidcUser(oidcUser);

        assertNotNull(result);
        assertEquals(newUserId, result.getUserId());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User newUser = captor.getValue();

        assertEquals("google-sub-new", newUser.getGoogleSub());
        assertEquals("obrisan@example.com", newUser.getEmail());
        assertEquals(UserStatus.ONBOARDING, newUser.getStatus());
        assertEquals(null, newUser.getRole());
        assertEquals(null, newUser.getOrganizerApproval());
    }

}
