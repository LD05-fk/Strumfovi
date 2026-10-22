package hr.fer.artconnect.auth;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
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
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

// Spring Session JDBC serijalizira principal zajedno sa SecurityContextom.
public class ArtConnectPrincipal implements OidcUser, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private final UUID userId;
    private final UserRole role;
    private final UserStatus status;
    private final OidcUser oidcUser;

    public ArtConnectPrincipal(User user, OidcUser oidcUser) {
        Objects.requireNonNull(user, "user ne smije biti null");
        this.oidcUser = Objects.requireNonNull(oidcUser, "oidcUser ne smije biti null");
        this.userId = Objects.requireNonNull(user.getId(), "userId ne smije biti null");
        this.role = user.getRole();
        this.status = Objects.requireNonNull(user.getStatus(), "user.status ne smije biti null");
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

    @Override
    public Map<String, Object> getAttributes() {
        return oidcUser.getAttributes();
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
        return oidcUser.getClaims();
    }

    @Override
    public OidcIdToken getIdToken() {
        return oidcUser.getIdToken();
    }

    @Override
    public @Nullable OidcUserInfo getUserInfo() {
        return oidcUser.getUserInfo();
    }

}
