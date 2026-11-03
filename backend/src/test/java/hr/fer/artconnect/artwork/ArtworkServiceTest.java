package hr.fer.artconnect.artwork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hr.fer.artconnect.artwork.domain.Artwork;
import hr.fer.artconnect.artwork.domain.ArtworkImage;
import hr.fer.artconnect.artwork.domain.ArtworkImageRepository;
import hr.fer.artconnect.artwork.domain.ArtworkRepository;
import hr.fer.artconnect.artwork.domain.ArtworkVisibility;
import hr.fer.artconnect.artwork.service.ArtworkMapper;
import hr.fer.artconnect.artwork.service.ArtworkService;
import hr.fer.artconnect.artwork.service.ArtworkVisibilityPolicy;
import hr.fer.artconnect.artwork.web.ArtworkDto;
import hr.fer.artconnect.artwork.web.ArtworkRequest;
import hr.fer.artconnect.auth.CurrentUserProvider;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRepository;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import hr.fer.artconnect.file.domain.StoredFile;
import hr.fer.artconnect.file.service.FileStorageService;
import hr.fer.artconnect.support.TestUsers;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArtworkServiceTest {
    private static final UUID ARTIST_ID = UUID.randomUUID();

    @Mock
    ArtworkRepository artworks;

    @Mock
    ArtworkImageRepository artworkImages;

    @Mock
    UserRepository users;

    @Mock
    CurrentUserProvider currentUserProvider;

    @Mock
    ArtworkMapper artworkMapper;

    @Mock
    ArtworkVisibilityPolicy visibilityPolicy;

    @Mock
    FileStorageService fileStorage;

    @Test
    void spremaDjeloNaPrijavljenogUmjetnikaBezSlika() {
        User artist = artist();
        var request = new ArtworkRequest(
                "  Morska tišina  ", 2024, "  Ulje na platnu  ", "  ", "  ", ArtworkVisibility.PUBLIC);
        var expected = mock(ArtworkDto.class);
        when(currentUserProvider.getCurrentUser()).thenReturn(artist);
        when(artworks.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(artworkMapper.toDto(any(), eq(List.of()))).thenReturn(expected);

        var result = service().create(request);

        assertThat(result).isSameAs(expected);
        var saved = ArgumentCaptor.forClass(Artwork.class);
        verify(artworks).save(saved.capture());
        assertThat(saved.getValue().getArtist()).isSameAs(artist);
        assertThat(saved.getValue().getTitle()).isEqualTo("Morska tišina");
        assertThat(saved.getValue().getYear()).isEqualTo((short) 2024);
        assertThat(saved.getValue().getTechnique()).isEqualTo("Ulje na platnu");
        assertThat(saved.getValue().getDimensions()).isNull();
        assertThat(saved.getValue().getDescription()).isNull();
        assertThat(saved.getValue().getVisibility()).isEqualTo(ArtworkVisibility.PUBLIC);
        verify(artworkMapper).toDto(saved.getValue(), List.of());
    }

    @Test
    void bezPrijavljenogKorisnikaNeSpremaDjelo() {
        when(currentUserProvider.getCurrentUser())
                .thenThrow(new ApiException(ErrorCode.UNAUTHENTICATED, "Potrebno je prijaviti se."));

        assertThatThrownBy(() -> service().create(
                new ArtworkRequest("Morska tišina", 2024, "Ulje", null, null, ArtworkVisibility.PRIVATE)))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.UNAUTHENTICATED);
        verify(artworks, never()).save(any());
        verify(artworkMapper, never()).toDto(any(), any());
    }

    @Test
    void azuriraVlastitoDjeloUzOcuvanjeSkrivenostiOdAdministratora() {
        User artist = artist();
        Artwork artwork = Artwork.create(artist, "Stari naslov", (short) 2020, "Akril",
                null, null, ArtworkVisibility.PUBLIC);
        ReflectionTestUtils.setField(artwork, "hiddenByAdmin", true);
        var request = new ArtworkRequest(
                "Novi naslov", 2024, "Ulje", "40 x 50 cm", "Novi opis", ArtworkVisibility.PRIVATE);
        var expected = mock(ArtworkDto.class);
        when(currentUserProvider.getCurrentUser()).thenReturn(artist);
        when(artworks.findById(ARTIST_ID)).thenReturn(Optional.of(artwork));
        when(artworks.save(artwork)).thenReturn(artwork);
        when(artworkImages.findAllWithFileByArtworkIds(any())).thenReturn(List.<ArtworkImage>of());
        when(artworkMapper.toDto(artwork, List.of())).thenReturn(expected);

        var result = service().update(ARTIST_ID, request);

        assertThat(result).isSameAs(expected);
        assertThat(artwork.getTitle()).isEqualTo("Novi naslov");
        assertThat(artwork.getYear()).isEqualTo((short) 2024);
        assertThat(artwork.getTechnique()).isEqualTo("Ulje");
        assertThat(artwork.getDimensions()).isEqualTo("40 x 50 cm");
        assertThat(artwork.getDescription()).isEqualTo("Novi opis");
        assertThat(artwork.getVisibility()).isEqualTo(ArtworkVisibility.PRIVATE);
        assertThat(artwork.isHiddenByAdmin()).isTrue();
        assertThat(artwork.getUpdatedAt()).isNotNull();
        verify(artworkMapper).toDto(artwork, List.of());
    }

    @Test
    void nepostojecaIliTudaDjelaVracajuNotFound() {
        User artist = artist();
        when(currentUserProvider.getCurrentUser()).thenReturn(artist);
        when(artworks.findById(ARTIST_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().update(ARTIST_ID, validRequest()))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.NOT_FOUND);

        Artwork anotherArtistArtwork = Artwork.create(
                TestUsers.user(UUID.randomUUID(), "other", "other@artconnect.test",
                        hr.fer.artconnect.auth.domain.UserRole.ARTIST,
                        hr.fer.artconnect.auth.domain.UserStatus.ACTIVE),
                "Tuđe djelo", (short) 2020, "Akril", null, null, ArtworkVisibility.PUBLIC);
        when(artworks.findById(ARTIST_ID)).thenReturn(Optional.of(anotherArtistArtwork));

        assertThatThrownBy(() -> service().update(ARTIST_ID, validRequest()))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(artworks, never()).save(any());
        verify(artworkMapper, never()).toDto(any(), any());
    }

    @Test
    void briseDjeloSlikeMetapodatkeDatoteka() {
        User artist = artist();
        Artwork artwork = Artwork.create(artist, "Naslov", (short) 2024, "Ulje",
                null, null, ArtworkVisibility.PUBLIC);
        ArtworkImage image = mock(ArtworkImage.class);
        StoredFile storedFile = mock(StoredFile.class);
        when(image.getFile()).thenReturn(storedFile);
        when(currentUserProvider.getCurrentUser()).thenReturn(artist);
        when(artworks.findById(ARTIST_ID)).thenReturn(Optional.of(artwork));
        when(artworkImages.findAllWithFileByArtworkIds(List.of(ARTIST_ID))).thenReturn(List.of(image));

        service().delete(ARTIST_ID);

        verify(artworkImages).deleteAll(List.of(image));
        verify(artworkImages).flush();
        verify(fileStorage).delete(storedFile);
        verify(artworks).delete(artwork);
    }

    @Test
    void nebriseTudeDjelo() {
        User artist = artist();
        User other = TestUsers.user(UUID.randomUUID(), "other", "other@artconnect.test",
                hr.fer.artconnect.auth.domain.UserRole.ARTIST,
                hr.fer.artconnect.auth.domain.UserStatus.ACTIVE);
        Artwork artwork = Artwork.create(other, "Tuđe djelo", (short) 2024, "Ulje",
                null, null, ArtworkVisibility.PUBLIC);
        when(currentUserProvider.getCurrentUser()).thenReturn(artist);
        when(artworks.findById(ARTIST_ID)).thenReturn(Optional.of(artwork));

        assertThatThrownBy(() -> service().delete(ARTIST_ID))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.NOT_FOUND);

        verify(artworkImages, never()).deleteAll(any());
        verify(fileStorage, never()).delete(any());
        verify(artworks, never()).delete(any());
    }

    private static ArtworkRequest validRequest() {
        return new ArtworkRequest("Naslov", 2024, "Ulje", null, null, ArtworkVisibility.PUBLIC);
    }

    private ArtworkService service() {
        return new ArtworkService(artworks, artworkImages, users,
                currentUserProvider, artworkMapper, visibilityPolicy, fileStorage);
    }

    private static User artist() {
        return TestUsers.user(ARTIST_ID, "google-umjetnik", "umjetnik@artconnect.test",
                hr.fer.artconnect.auth.domain.UserRole.ARTIST,
                hr.fer.artconnect.auth.domain.UserStatus.ACTIVE);
    }
}
