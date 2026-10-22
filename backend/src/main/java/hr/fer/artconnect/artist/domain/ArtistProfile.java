package hr.fer.artconnect.artist.domain;

import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.catalog.domain.ArtField;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "artist_profiles")
public class ArtistProfile {
    @Id
    private UUID userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String biography;

    private String artistStatement;

    private OffsetDateTime updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "artist_profile_art_fields",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "art_field_id"))
    private Set<ArtField> artFields = new LinkedHashSet<>();

    protected ArtistProfile() {}

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

    public String getBiography() {
        return biography;
    }

    public String getArtistStatement() {
        return artistStatement;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Set<ArtField> getArtFields() {
        return artFields;
    }
}
