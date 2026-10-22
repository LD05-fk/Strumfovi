package hr.fer.artconnect.auth;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRepository;
import hr.fer.artconnect.auth.domain.UserStatus;

@Service
public class ArtConnectOidcUserService extends OidcUserService {
    private final UserRepository userRepository;

    public ArtConnectOidcUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public ArtConnectPrincipal loadUser(OidcUserRequest userRequest) {
        var oidcUser = super.loadUser(userRequest);
        return processOidcUser(oidcUser);
    }
    @Transactional
    ArtConnectPrincipal processOidcUser(OidcUser oidcUser) {
        if (!Boolean.TRUE.equals(oidcUser.getEmailVerified())) {
            throw new OAuth2AuthenticationException(new OAuth2Error("email_not_verified"),
                    "Google račun nema potvrđenu e-mail adresu.");

        }

        String rawEmail = oidcUser.getEmail();
        String sub = oidcUser.getSubject();
        String avatarUrl = oidcUser.getPicture();
        String displayName = oidcUser.getFullName();

        if (rawEmail == null || rawEmail.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("email_missing"),
                    "Google račun nema e-mail adresu.");
        }
        String email = rawEmail.toLowerCase(java.util.Locale.ROOT);

        //ako nema imena umjesto njega se stavlja email
        if (displayName == null || displayName.isBlank()) {
            displayName = email;
        }

        if (displayName.length() > 120) {
            displayName = displayName.substring(0, 120);
        }
        var existingUser = userRepository.findByGoogleSub(sub);

        if (existingUser.isPresent()) {
            var user = existingUser.get();
            user.updateGoogleProfile(email, displayName, avatarUrl);
            return new ArtConnectPrincipal(user, oidcUser);
        }
        var userByEmail = userRepository.findByEmailIgnoreCase(email);
        if (userByEmail.isPresent()) {
            var user = userByEmail.get();

            if (user.getStatus() != UserStatus.INVITED) {
                throw new OAuth2AuthenticationException(new OAuth2Error("account_conflict"),
                        "Postoji račun s ovom e-mail adresom koji nije pozvan.");
            }
            
            user.activateInvitedGoogleAccount(sub, email, displayName, avatarUrl);
            return new ArtConnectPrincipal(user, oidcUser);
        }
        var user = User.createOnboardingUser(sub, email, displayName, avatarUrl);
        user = userRepository.save(user);
        return new ArtConnectPrincipal(user, oidcUser);

    }
}
