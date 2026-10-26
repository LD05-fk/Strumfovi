package hr.fer.artconnect.artwork.service;

import hr.fer.artconnect.artwork.domain.Artwork;
import hr.fer.artconnect.artwork.domain.ArtworkImage;
import hr.fer.artconnect.artwork.web.ArtworkDto;
import hr.fer.artconnect.artwork.web.ArtworkImageDto;
import hr.fer.artconnect.file.service.StoredFileDto;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ArtworkMapper {
    public ArtworkDto toDto(Artwork artwork, List<ArtworkImage> images) {
        List<ArtworkImageDto> imageDtos = images.stream()
                .map(image -> new ArtworkImageDto(
                        image.getId(), StoredFileDto.from(image.getFile()), image.getAltText()))
                .toList();
        return new ArtworkDto(
                artwork.getId(),
                artwork.getArtist().getId(),
                artwork.getTitle(),
                artwork.getYear(),
                artwork.getTechnique(),
                artwork.getDimensions(),
                artwork.getDescription(),
                artwork.getVisibility(),
                artwork.isHiddenByAdmin(),
                imageDtos,
                artwork.getCreatedAt());
    }
}
