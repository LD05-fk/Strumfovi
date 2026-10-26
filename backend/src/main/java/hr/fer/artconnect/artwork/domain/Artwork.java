package hr.fer.artconnect.artwork.domain;

import hr.fer.artconnect.auth.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "artworks")
public class Artwork {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artist_id", nullable = false)
    private User artist;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false)
    private short year;

    @Column(nullable = false, length = 200)
    private String technique;

    @Column(length = 100)
    private String dimensions;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ArtworkVisibility visibility = ArtworkVisibility.PRIVATE;

    @Column(nullable = false)
    private boolean hiddenByAdmin;

    private OffsetDateTime hiddenAt;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    protected Artwork() {}

    public static Artwork create(User artist, String title, short year, String technique, String dimensions,
            String description, ArtworkVisibility visibility) {
        Artwork artwork = new Artwork();
        artwork.artist = artist;
        artwork.title = title;
        artwork.year = year;
        artwork.technique = technique;
        artwork.dimensions = dimensions;
        artwork.description = description;
        artwork.visibility = visibility;
        return artwork;
    }

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

    public User getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public short getYear() {
        return year;
    }

    public String getTechnique() {
        return technique;
    }

    public String getDimensions() {
        return dimensions;
    }

    public String getDescription() {
        return description;
    }

    public ArtworkVisibility getVisibility() {
        return visibility;
    }

    public boolean isHiddenByAdmin() {
        return hiddenByAdmin;
    }

    public OffsetDateTime getHiddenAt() {
        return hiddenAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
