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
import java.util.Set;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class OnboardingEnforcementFilter extends OncePerRequestFilter {
    private static final Set<String> ALLOWED = Set.of(
            "GET /api/v1/me", "DELETE /api/v1/me", "POST /api/v1/me/role",
            "POST /api/v1/auth/logout", "GET /api/v1/auth/csrf");

    private final UserRepository users;
    private final SecurityErrorHandler errors;

    public OnboardingEnforcementFilter(UserRepository users, SecurityErrorHandler errors) {
        this.users = users;
        this.errors = errors;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (path.startsWith("/api/v1/") && !ALLOWED.contains(request.getMethod() + " " + path)
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
