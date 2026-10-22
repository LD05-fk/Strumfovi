package hr.fer.artconnect.auth;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

// Spring Session JDBC serijalizira principal zajedno sa SecurityContextom.
public class ArtConnectPrincipal implements OidcUser, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID userId;
    private final UserRole role;
    private final UserStatus status;
    private final @Nullable OidcUser oidcUser;
    private final Map<String, Object> claims;

    public ArtConnectPrincipal(User user, OidcUser oidcUser) {
        this(user, Objects.requireNonNull(oidcUser, "oidcUser ne smije biti null"), Map.of());
    }

    // Prijava bez Googlea (POST /dev/login-as): claimovi se grade iz reda users, a ID tokena nema.
    public ArtConnectPrincipal(User user) {
        this(user, null, claimsFrom(Objects.requireNonNull(user, "user ne smije biti null")));
    }

    private ArtConnectPrincipal(User user, @Nullable OidcUser oidcUser, Map<String, Object> claims) {
        Objects.requireNonNull(user, "user ne smije biti null");
        this.oidcUser = oidcUser;
        this.claims = claims;
        this.userId = Objects.requireNonNull(user.getId(), "userId ne smije biti null");
        this.role = user.getRole();
        this.status = Objects.requireNonNull(user.getStatus(), "user.status ne smije biti null");
    }

    private static Map<String, Object> claimsFrom(User user) {
        var claims = new LinkedHashMap<String, Object>();
        claims.put(IdTokenClaimNames.SUB,
                user.getGoogleSub() != null ? user.getGoogleSub() : String.valueOf(user.getId()));
        if (user.getEmail() != null) {
            claims.put(StandardClaimNames.EMAIL, user.getEmail());
        }
        if (user.getDisplayName() != null) {
            claims.put(StandardClaimNames.NAME, user.getDisplayName());
        }
        if (user.getAvatarUrl() != null) {
            claims.put(StandardClaimNames.PICTURE, user.getAvatarUrl());
        }
        return Collections.unmodifiableMap(claims);
    }

    public UUID getUserId() {
        return userId;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    // Izvorni Googleov korisnik; null kad principal nije nastao Google prijavom.
    @Nullable OidcUser getOidcUser() {
        return oidcUser;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oidcUser != null ? oidcUser.getAttributes() : claims;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role == null ? List.of()
                : List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getName() {
        return userId.toString();
    }

    @Override
    public Map<String, Object> getClaims() {
        return oidcUser != null ? oidcUser.getClaims() : claims;
    }

    @Override
    public @Nullable OidcIdToken getIdToken() {
        return oidcUser != null ? oidcUser.getIdToken() : null;
    }

    @Override
    public @Nullable OidcUserInfo getUserInfo() {
        return oidcUser != null ? oidcUser.getUserInfo() : null;
    }

}
