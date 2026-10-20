package hr.fer.artconnect.artwork.domain;

import hr.fer.artconnect.file.domain.StoredFile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "artwork_images")
public class ArtworkImage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artwork_id", nullable = false)
    private Artwork artwork;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false, unique = true)
    private StoredFile file;

    @Column(length = 300)
    private String altText;

    @Column(nullable = false)
    private short position;

    protected ArtworkImage() {}

    @PrePersist
    void onCreate() {
        if (altText == null && artwork != null) {
            altText = artwork.getTitle();
        }
    }

    public UUID getId() {
        return id;
    }

    public Artwork getArtwork() {
        return artwork;
    }

    public StoredFile getFile() {
        return file;
    }

    public String getAltText() {
        return altText;
    }

    public short getPosition() {
        return position;
    }
}
