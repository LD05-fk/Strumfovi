package hr.fer.artconnect.artwork.web;

import hr.fer.artconnect.artwork.domain.ArtworkVisibility;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Shema Artwork. */
public record ArtworkDto(
        UUID id,
        UUID artistId,
        String title,
        short year,
        String technique,
        String dimensions,
        String description,
        ArtworkVisibility visibility,
        boolean hiddenByAdmin,
        List<ArtworkImageDto> images,
        OffsetDateTime createdAt
) {
    public ArtworkDto {
        images = List.copyOf(images);
    }
}
