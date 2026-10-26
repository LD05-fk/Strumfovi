package hr.fer.artconnect.artwork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hr.fer.artconnect.artwork.domain.Artwork;
import hr.fer.artconnect.artwork.domain.ArtworkImage;
import hr.fer.artconnect.artwork.domain.ArtworkVisibility;
import hr.fer.artconnect.artwork.service.ArtworkMapper;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.file.domain.StoredFile;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ArtworkMapperTest {
    @Test
    void mapiraDjeloISlikeUShemeOdgovora() {
        UUID artistId = UUID.randomUUID();
        UUID artworkId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-01-02T03:04:05Z");
        User artist = mock(User.class);
        Artwork artwork = mock(Artwork.class);
        ArtworkImage image = mock(ArtworkImage.class);
        StoredFile file = mock(StoredFile.class);
        when(artist.getId()).thenReturn(artistId);
        when(artwork.getId()).thenReturn(artworkId);
        when(artwork.getArtist()).thenReturn(artist);
        when(artwork.getTitle()).thenReturn("Morska tišina");
        when(artwork.getYear()).thenReturn((short) 2024);
        when(artwork.getTechnique()).thenReturn("Ulje");
        when(artwork.getDimensions()).thenReturn("40 x 50 cm");
        when(artwork.getDescription()).thenReturn("Opis");
        when(artwork.getVisibility()).thenReturn(ArtworkVisibility.PUBLIC);
        when(artwork.isHiddenByAdmin()).thenReturn(false);
        when(artwork.getCreatedAt()).thenReturn(createdAt);
        when(image.getId()).thenReturn(imageId);
        when(image.getFile()).thenReturn(file);
        when(image.getAltText()).thenReturn("Slika mora");
        when(file.getId()).thenReturn(fileId);
        when(file.getOriginalName()).thenReturn("morska.jpg");
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getSizeBytes()).thenReturn(1234L);

        var result = new ArtworkMapper().toDto(artwork, List.of(image));

        assertThat(result.id()).isEqualTo(artworkId);
        assertThat(result.artistId()).isEqualTo(artistId);
        assertThat(result.title()).isEqualTo("Morska tišina");
        assertThat(result.year()).isEqualTo((short) 2024);
        assertThat(result.technique()).isEqualTo("Ulje");
        assertThat(result.visibility()).isEqualTo(ArtworkVisibility.PUBLIC);
        assertThat(result.hiddenByAdmin()).isFalse();
        assertThat(result.createdAt()).isEqualTo(createdAt);
        assertThat(result.images()).hasSize(1);
        assertThat(result.images().get(0).id()).isEqualTo(imageId);
        assertThat(result.images().get(0).altText()).isEqualTo("Slika mora");
        assertThat(result.images().get(0).file().id()).isEqualTo(fileId);
        assertThat(result.images().get(0).file().originalName()).isEqualTo("morska.jpg");
        assertThat(result.images().get(0).file().url()).isEqualTo("/api/v1/files/" + fileId);
    }

    @Test
    void mapiraPrazanNizSlika() {
        Artwork artwork = mock(Artwork.class);
        when(artwork.getArtist()).thenReturn(mock(User.class));
        var result = new ArtworkMapper().toDto(artwork, List.of());

        assertThat(result.images()).isEmpty();
    }
}
