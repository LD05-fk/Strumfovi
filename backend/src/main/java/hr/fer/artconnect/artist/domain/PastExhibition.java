package hr.fer.artconnect.artist.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "past_exhibitions")
public class PastExhibition {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artist_id")
    private ArtistProfile artistProfile;

    private String title;

    private short year;

    private String place;

    private OffsetDateTime createdAt;

    protected PastExhibition() {}

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public ArtistProfile getArtistProfile() {
        return artistProfile;
    }

    public String getTitle() {
        return title;
    }

    public short getYear() {
        return year;
    }

    public String getPlace() {
        return place;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public static PastExhibition create(ArtistProfile artistProfile, String title, short year, String place) {
        PastExhibition exhibition = new PastExhibition();
        exhibition.artistProfile = artistProfile;
        exhibition.title = title;
        exhibition.year = year;
        exhibition.place = place;
        return exhibition;
    }
}
