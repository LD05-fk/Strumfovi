package hr.fer.artconnect.admin.service;

import hr.fer.artconnect.admin.web.AdminUser;
import hr.fer.artconnect.auth.domain.User;

public final class AdminUserMapper {
    private AdminUserMapper() {
    }

    public static AdminUser toAdminUser(User user, String organizationName) {
        return new AdminUser(
                user.getId(),
                user.getDisplayName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getOrganizerApproval(),
                organizationName,
                user.getCreatedAt(),
                user.getLastLoginAt());
    }
}
