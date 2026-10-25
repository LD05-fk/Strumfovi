package hr.fer.artconnect.auth.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String googleSub;

    private String email;

    private String displayName;

    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    private OrganizerApproval organizerApproval;

    private OffsetDateTime blockedAt;

    private OffsetDateTime deletedAt;

    private OffsetDateTime lastLoginAt;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    protected User() {}

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public String getGoogleSub() {
        return googleSub;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public OrganizerApproval getOrganizerApproval() {
        return organizerApproval;
    }

    public OffsetDateTime getBlockedAt() {
        return blockedAt;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public static User createOnboardingUser(String googleSub, String email, String displayName, String avatarUrl){
        User user = new User();
        user.googleSub = googleSub;
        user.email = email;
        user.displayName = displayName;
        user.avatarUrl = avatarUrl;
        user.role = null;
        user.status = UserStatus.ONBOARDING;
        user.organizerApproval = null;
        user.lastLoginAt = OffsetDateTime.now();
        return user;
    }

    public void updateGoogleProfile(String email, String displayName, String avatarUrl) {
        this.email = email;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.lastLoginAt = OffsetDateTime.now();
    }

    public void loginAsDev() {
        if (this.status == UserStatus.INVITED) {
            this.status = UserStatus.ACTIVE;
        }
        this.lastLoginAt = OffsetDateTime.now();
    }

    public void activateInvitedGoogleAccount(String googleSub, String email, String displayName, String avatarUrl){
        this.googleSub = googleSub;
        this.email = email;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.status = UserStatus.ACTIVE;
        this.lastLoginAt = OffsetDateTime.now();
    }
}
