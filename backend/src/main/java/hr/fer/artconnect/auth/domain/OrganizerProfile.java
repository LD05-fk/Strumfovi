package hr.fer.artconnect.auth.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "organizer_profiles")
public class OrganizerProfile {
    @Id
    private UUID userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String organizationName;

    private OffsetDateTime updatedAt;

    protected OrganizerProfile() {}

    public static OrganizerProfile createFor(User user) {
        OrganizerProfile profile = new OrganizerProfile();
        profile.user = user;
        return profile;
    }

    @PrePersist
    void onCreate() {
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getUserId() {
        return userId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void updateOrganizationName(String organizationName) {
        this.organizationName = organizationName;
        this.updatedAt = OffsetDateTime.now();
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
