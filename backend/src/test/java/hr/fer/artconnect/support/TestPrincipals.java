package hr.fer.artconnect.support;

import java.time.Instant;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import hr.fer.artconnect.auth.ArtConnectPrincipal;
import hr.fer.artconnect.auth.domain.User;

// Principal kakav nastaje nakon Google prijave, bez stvarnog poziva Googleu.
public final class TestPrincipals {

    private TestPrincipals() {
    }

    public static ArtConnectPrincipal principal(User user) {
        Instant now = Instant.now();
        OidcIdToken idToken = OidcIdToken.withTokenValue("test-id-token")
                .subject("google-" + user.getId())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .build();
        return new ArtConnectPrincipal(user, new DefaultOidcUser(List.of(), idToken));
    }

    public static UsernamePasswordAuthenticationToken authentication(User user) {
        ArtConnectPrincipal principal = principal(user);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities());
    }
}
