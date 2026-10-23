package hr.fer.artconnect.auth.web;

import java.util.UUID;

import hr.fer.artconnect.auth.domain.OrganizerApproval;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;

public record DevLoginResponse(
        UUID id,
        String displayName,
        String email,
        String avatarUrl,
        UserRole role,
        UserStatus status,
        OrganizerApproval organizerApproval) {

    public static DevLoginResponse from(User user) {
        return new DevLoginResponse(user.getId(), user.getDisplayName(), user.getEmail(), user.getAvatarUrl(),
                user.getRole(), user.getStatus(), user.getOrganizerApproval());
    }
}
