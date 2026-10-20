package hr.fer.artconnect.admin.web;

import hr.fer.artconnect.auth.domain.OrganizerApproval;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUser(
        UUID id,
        String displayName,
        String email,
        UserRole role,
        UserStatus status,
        OrganizerApproval organizerApproval,
        String organizationName,
        OffsetDateTime createdAt,
        OffsetDateTime lastLoginAt) {
}
