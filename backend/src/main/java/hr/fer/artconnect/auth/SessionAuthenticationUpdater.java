package hr.fer.artconnect.auth;

import java.util.UUID;

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

        Authentication currentAuthentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (currentAuthentication == null
                || !(currentAuthentication.getPrincipal() instanceof OidcUser oidcUser)) {
            throw new IllegalStateException(
                    "U trenutnom SecurityContextu nema OIDC korisnika.");
        }

        ArtConnectPrincipal principal =
                new ArtConnectPrincipal(user, oidcUser);

        Authentication updatedAuthentication =
                new org.springframework.security.authentication
                        .UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                principal.getAuthorities());

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();
        context.setAuthentication(updatedAuthentication);

        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        return principal;
    }
}