package hr.fer.artconnect.auth.service;

import hr.fer.artconnect.auth.CurrentUserProvider;
import hr.fer.artconnect.auth.domain.OrganizerApproval;
import hr.fer.artconnect.auth.domain.OrganizerProfile;
import hr.fer.artconnect.auth.domain.OrganizerProfileRepository;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRepository;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoleSelectionService {
    private final CurrentUserProvider currentUserProvider;
    private final UserRepository users;
    private final OrganizerProfileRepository organizerProfiles;

    public RoleSelectionService(CurrentUserProvider currentUserProvider, UserRepository users,
            OrganizerProfileRepository organizerProfiles) {
        this.currentUserProvider = currentUserProvider;
        this.users = users;
        this.organizerProfiles = organizerProfiles;
    }

    @Transactional
    public User chooseRole(UserRole role) {
        if (role != UserRole.ARTIST && role != UserRole.ORGANIZER) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Odaberite ulogu ARTIST ili ORGANIZER.");
        }
        User currentUser = currentUserProvider.getCurrentUser();
        OrganizerApproval approval = role == UserRole.ORGANIZER
                ? OrganizerApprovalPolicy.defaultForNewOrganizer() : null;
        int updated = users.chooseRole(currentUser.getId(), role, approval, OffsetDateTime.now());
        if (updated == 0) {
            throw new ApiException(ErrorCode.ROLE_ALREADY_CHOSEN, "Uloga je već odabrana.");
        }
        // Skupno ažuriranje prazni JPA kontekst, pa ponovno čitamo aktualni red.
        User user = users.findById(currentUser.getId()).orElseThrow(() ->
                new ApiException(ErrorCode.UNAUTHENTICATED, "Potrebno je prijaviti se."));
        if (role == UserRole.ORGANIZER) {
            organizerProfiles.save(OrganizerProfile.createFor(user));
        }
        return user;
    }
}
