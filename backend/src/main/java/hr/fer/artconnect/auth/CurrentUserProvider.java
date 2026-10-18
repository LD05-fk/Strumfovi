package hr.fer.artconnect.auth;

import hr.fer.artconnect.common.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.context.*;
import org.springframework.stereotype.*;

@Service
public class CurrentUserProvider {
    // Servisi ovako dohvaćaju korisnika za provjere vlasništva, bez pristupa HTTP zahtjevu.
    public CurrentUser getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || new AuthenticationTrustResolverImpl().isAnonymous(authentication)
                || !(authentication.getPrincipal() instanceof CurrentUser user)) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED, "Potrebno je prijaviti se.");
        }
        return user;
    }
}
