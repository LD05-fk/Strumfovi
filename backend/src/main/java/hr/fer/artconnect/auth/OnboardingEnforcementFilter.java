package hr.fer.artconnect.auth;

import hr.fer.artconnect.auth.domain.UserRepository;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.common.ErrorCode;
import hr.fer.artconnect.common.SecurityErrorHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

public class OnboardingEnforcementFilter extends OncePerRequestFilter {
    // Istu obradu kodiranih segmenata putanje koristi Springovo usmjeravanje zahtjeva.
    private static final PathPatternRequestMatcher.Builder PATHS = PathPatternRequestMatcher.withDefaults();
    private static final RequestMatcher API = PATHS.matcher("/api/v1/**");
    private static final RequestMatcher ALLOWED = new OrRequestMatcher(
            PATHS.matcher(HttpMethod.GET, "/api/v1/me"),
            PATHS.matcher(HttpMethod.DELETE, "/api/v1/me"),
            PATHS.matcher(HttpMethod.POST, "/api/v1/me/role"),
            PATHS.matcher(HttpMethod.POST, "/api/v1/auth/logout"),
            PATHS.matcher(HttpMethod.GET, "/api/v1/auth/csrf"));

    private final UserRepository users;
    private final SecurityErrorHandler errors;

    public OnboardingEnforcementFilter(UserRepository users, SecurityErrorHandler errors) {
        this.users = users;
        this.errors = errors;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (API.matches(request) && !ALLOWED.matches(request)
                && authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof ArtConnectPrincipal principal) {
            // Status iz sesije može biti zastario, zato odlučujemo prema bazi.
            var user = users.findById(principal.getUserId());
            if (user.isEmpty()) {
                errors.write(response, ErrorCode.UNAUTHENTICATED);
                return;
            }
            if (user.get().getStatus() == UserStatus.ONBOARDING) {
                errors.write(response, ErrorCode.ONBOARDING_REQUIRED);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
