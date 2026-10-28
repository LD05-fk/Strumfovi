package hr.fer.artconnect.auth.service;

import hr.fer.artconnect.auth.CurrentUserProvider;
import hr.fer.artconnect.auth.domain.OrganizerProfile;
import hr.fer.artconnect.auth.domain.OrganizerProfileRepository;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.auth.web.OrganizerProfileDto;
import hr.fer.artconnect.auth.web.OrganizerProfileUpdateRequest;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizerProfileService {
    private final OrganizerProfileRepository organizerProfiles;
    private final CurrentUserProvider currentUserProvider;

    public OrganizerProfileService(OrganizerProfileRepository organizerProfiles,
            CurrentUserProvider currentUserProvider) {
        this.organizerProfiles = organizerProfiles;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public OrganizerProfileDto getMyOrganizerProfile() {
        User user = getActiveUser();
        OrganizerProfile profile = organizerProfiles.findById(user.getId()).orElse(null);
        return toDto(user, profile);
    }

    @Transactional
    public OrganizerProfileDto updateMyOrganizerProfile(OrganizerProfileUpdateRequest request) {
        User user = getActiveUser();
        String organizationName = normalize(request.organizationName());
        organizerProfiles.upsert(user.getId(), organizationName);
        OrganizerProfile saved = organizerProfiles.findById(user.getId()).orElseThrow();
        return toDto(user, saved);
    }

    private User getActiveUser() {
        User user = currentUserProvider.getCurrentUser();
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new ApiException(ErrorCode.USER_BLOCKED, "Korisnički račun je blokiran.");
        }
        return user;
    }

    private static String normalize(String organizationName) {
        if (organizationName == null) {
            return null;
        }
        String trimmed = organizationName.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static OrganizerProfileDto toDto(User user, OrganizerProfile profile) {
        return new OrganizerProfileDto(
                user.getId(),
                user.getDisplayName(),
                profile == null ? null : profile.getOrganizationName(),
                user.getOrganizerApproval());
    }
}
