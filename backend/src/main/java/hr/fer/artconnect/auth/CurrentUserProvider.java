package hr.fer.artconnect.auth;

import hr.fer.artconnect.auth.domain.*;
import hr.fer.artconnect.common.*;
import java.util.Optional;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.*;
import org.springframework.stereotype.*;

@Service
public class CurrentUserProvider {
    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Servisi ovako dohvaćaju korisnika za provjere vlasništva, bez pristupa HTTP zahtjevu.
    public User getCurrentUser() {
        return getOptionalCurrentUser().orElseThrow(CurrentUserProvider::unauthenticated);
    }

    public Optional<User> getOptionalCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || new AuthenticationTrustResolverImpl().isAnonymous(authentication)
                || !(authentication.getPrincipal() instanceof ArtConnectPrincipal principal)) {
            return Optional.empty();
        }
        // Sesija može nadživjeti korisnika, pa red uvijek čitamo iz baze.
        return userRepository.findById(principal.getUserId());
    }

    private static ApiException unauthenticated() {
        return new ApiException(ErrorCode.UNAUTHENTICATED, "Potrebno je prijaviti se.");
    }
}
