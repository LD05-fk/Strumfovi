package hr.fer.artconnect.auth;

import java.util.UUID;

import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class SessionAuthenticationUpdater {

    private final UserRepository userRepository;
    private final SecurityContextRepository securityContextRepository;

    public SessionAuthenticationUpdater(
            UserRepository userRepository,
            SecurityContextRepository securityContextRepository) {
        this.userRepository = userRepository;
        this.securityContextRepository = securityContextRepository;
    }

    @Transactional(readOnly = true)
    public ArtConnectPrincipal update(
            UUID userId,
            HttpServletRequest request,
            HttpServletResponse response) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Korisnik ne postoji: " + userId));

        // Principal se uvijek gradi iz reda users; trenutna prijava nije uvjet (POST /dev/login-as je anoniman).
        OidcUser googleUser = currentGoogleUser(userId);
        ArtConnectPrincipal principal = googleUser != null
                ? new ArtConnectPrincipal(user, googleUser)
                : new ArtConnectPrincipal(user);

        Authentication updatedAuthentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();
        context.setAuthentication(updatedAuthentication);

        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        return principal;
    }

    // Googleove podatke zadržavamo samo kad sesija već pripada istom korisniku (npr. odabir uloge).
    // Omotavamo izvornog Googleova korisnika, a ne stari principal, da se lanac ne produljuje.
    private static @Nullable OidcUser currentGoogleUser(UUID userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.getPrincipal() instanceof ArtConnectPrincipal current
                && current.getUserId().equals(userId)) {
            return current.getOidcUser();
        }
        return null;
    }
}
