package hr.fer.artconnect.auth.web;

import hr.fer.artconnect.auth.domain.OrganizerApproval;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import java.util.UUID;

public record CurrentUserDto(UUID id, String displayName, String email, String avatarUrl,
        UserRole role, UserStatus status, OrganizerApproval organizerApproval) {

    public static CurrentUserDto from(User user) {
        return new CurrentUserDto(user.getId(), user.getDisplayName(), user.getEmail(), user.getAvatarUrl(),
                user.getRole(), user.getStatus(), user.getOrganizerApproval());
    }
}
