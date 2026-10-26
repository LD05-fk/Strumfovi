package hr.fer.artconnect.artist.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "artist_links")
public class ArtistLink {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private ArtistProfile artistProfile;

    private String label;

    private String url;

    private short position;

    protected ArtistLink() {}

    public UUID getId() {
        return id;
    }

    public ArtistProfile getArtistProfile() {
        return artistProfile;
    }

    public String getLabel() {
        return label;
    }

    public String getUrl() {
        return url;
    }

    public short getPosition() {
        return position;
    }

    public static ArtistLink create(ArtistProfile artistProfile, String label, String url, short position) {
        ArtistLink link = new ArtistLink();
        link.artistProfile = artistProfile;
        link.label = label;
        link.url = url;
        link.position = position;
        return link;
    }
}
