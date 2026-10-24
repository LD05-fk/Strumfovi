package hr.fer.artconnect.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import hr.fer.artconnect.auth.domain.*;
import hr.fer.artconnect.common.ErrorCode;
import hr.fer.artconnect.common.SecurityErrorHandler;
import hr.fer.artconnect.support.TestPrincipals;
import hr.fer.artconnect.support.TestUsers;
import jakarta.servlet.FilterChain;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class OnboardingEnforcementFilterTest {
    private final UserRepository users = mock(UserRepository.class);
    private final SecurityErrorHandler errors = mock(SecurityErrorHandler.class);
    private final FilterChain chain = mock(FilterChain.class);
    private final OnboardingEnforcementFilter filter = new OnboardingEnforcementFilter(users, errors);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void dopusteneMetodeIPutanjeProlazeBezProvjereStatusa() throws Exception {
        login(UserStatus.ONBOARDING);
        for (String route : new String[] {"GET /api/v1/me", "DELETE /api/v1/me", "POST /api/v1/me/role",
                "POST /api/v1/auth/logout", "GET /api/v1/auth/csrf", "GET /onboarding"}) {
            String[] parts = route.split(" ");
            filter.doFilter(new MockHttpServletRequest(parts[0], parts[1]), new MockHttpServletResponse(), chain);
        }
        verify(chain, times(6)).doFilter(any(), any());
        verifyNoInteractions(users, errors);
    }

    @Test
    void anonimniPosjetiteljProlaziBezProvjereStatusa() throws Exception {
        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/artists"),
                new MockHttpServletResponse(), chain);
        verify(chain).doFilter(any(), any());
        verifyNoInteractions(users, errors);
    }

    @Test
    void statusIzBazeImaPrednostPredSesijomIIznimkeNeVrijedeZaDrugeMetode() throws Exception {
        User user = login(UserStatus.ACTIVE);
        var onboarding = TestUsers.user(user.getId(), "google", "test@example.com", null, UserStatus.ONBOARDING);
        when(users.findById(user.getId())).thenReturn(Optional.of(onboarding));
        for (String route : new String[] {"GET /api/v1/me/organizer-profile", "GET /api/v1/artists",
                "POST /api/v1/me", "GET /api/v1/me/role", "GET /api/v1/auth/logout", "POST /api/v1/auth/csrf"}) {
            String[] parts = route.split(" ");
            filter.doFilter(new MockHttpServletRequest(parts[0], parts[1]), new MockHttpServletResponse(), chain);
        }
        verify(errors, times(6)).write(any(), eq(ErrorCode.ONBOARDING_REQUIRED));
        verifyNoInteractions(chain);
    }

    @Test
    void aktivniRedUBaziProlaziIakoJeSesijaJosOnboardingANepostojeciVraca401() throws Exception {
        User user = login(UserStatus.ONBOARDING);
        var active = TestUsers.user(user.getId(), "google", "test@example.com", UserRole.ARTIST, UserStatus.ACTIVE);
        when(users.findById(user.getId())).thenReturn(Optional.of(active), Optional.empty());
        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/artists"),
                new MockHttpServletResponse(), chain);
        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/artists"),
                new MockHttpServletResponse(), chain);
        verify(chain, times(1)).doFilter(any(), any());
        verify(errors).write(any(), eq(ErrorCode.UNAUTHENTICATED));
    }

    private User login(UserStatus status) {
        User user = TestUsers.user(UUID.randomUUID(), "google", "test@example.com", null, status);
        SecurityContextHolder.getContext().setAuthentication(TestPrincipals.authentication(user));
        return user;
    }
}
