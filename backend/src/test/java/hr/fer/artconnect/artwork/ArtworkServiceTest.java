package hr.fer.artconnect.artwork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hr.fer.artconnect.catalog.service.SettingsService;
import hr.fer.artconnect.catalog.web.UploadLimits;
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
import hr.fer.artconnect.file.domain.FilePurpose;
import hr.fer.artconnect.file.domain.StoredFile;
import hr.fer.artconnect.file.service.FileStorageService;
import hr.fer.artconnect.file.service.FileTooLargeException;
import hr.fer.artconnect.file.service.UnsupportedFileTypeException;
import hr.fer.artconnect.support.TestUsers;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

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
    SettingsService settings;

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
    void spremaSlikuNaSljedecuPozicijuIZadaniAltText() {
        User artist = artist();
        UUID artworkId = UUID.randomUUID();
        Artwork artwork = artwork(artist);
        StoredFile storedFile = mock(StoredFile.class);
        var file = new MockMultipartFile("file", "slika.png", "image/png", new byte[] {1});
        when(currentUserProvider.getCurrentUser()).thenReturn(artist);
        when(artworks.findByIdForUpdate(artworkId)).thenReturn(Optional.of(artwork));
        when(settings.getUploadLimits()).thenReturn(limits(3));
        when(artworkImages.countByArtwork_Id(artworkId)).thenReturn(1L);
        when(artworkImages.findMaxPositionByArtworkId(artworkId)).thenReturn((short) 0);
        when(fileStorage.store(artist.getId(), FilePurpose.ARTWORK_IMAGE, file,
                Set.of("image/jpeg", "image/png"), 5242880)).thenReturn(storedFile);
        when(artworkImages.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service().uploadImage(artworkId, file, null);

        var saved = ArgumentCaptor.forClass(ArtworkImage.class);
        verify(artworkImages).save(saved.capture());
        assertThat(saved.getValue().getPosition()).isEqualTo((short) 1);
        assertThat(saved.getValue().getAltText()).isEqualTo("Morska tišina");
        assertThat(result.altText()).isEqualTo("Morska tišina");
        verify(artworks).findByIdForUpdate(artworkId);
    }

    @Test
    void limitBrojaSlikaSeProvjeravaPrijePohraneDatoteke() {
        User artist = artist();
        UUID artworkId = UUID.randomUUID();
        when(currentUserProvider.getCurrentUser()).thenReturn(artist);
        when(artworks.findByIdForUpdate(artworkId)).thenReturn(Optional.of(artwork(artist)));
        when(settings.getUploadLimits()).thenReturn(limits(1));
        when(artworkImages.countByArtwork_Id(artworkId)).thenReturn(1L);

        assertThatThrownBy(() -> service().uploadImage(artworkId,
                new MockMultipartFile("file", "large.jpg", "image/jpeg", new byte[] {1}), null))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.IMAGE_LIMIT_REACHED);
        verify(fileStorage, never()).store(any(), any(), any(), any(), anyLong());
    }

    @Test
    void vlasnistvoSeProvjeravaPrijeLimitaSlika() {
        User artist = artist();
        User otherArtist = TestUsers.user(UUID.randomUUID(), "drugi", "drugi@artconnect.test",
                hr.fer.artconnect.auth.domain.UserRole.ARTIST,
                hr.fer.artconnect.auth.domain.UserStatus.ACTIVE);
        UUID artworkId = UUID.randomUUID();
        when(currentUserProvider.getCurrentUser()).thenReturn(artist);
        when(artworks.findByIdForUpdate(artworkId)).thenReturn(Optional.of(artwork(otherArtist)));

        assertThatThrownBy(() -> service().uploadImage(artworkId,
                new MockMultipartFile("file", "slika.jpg", "image/jpeg", new byte[] {1}), null))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(artworkImages, never()).countByArtwork_Id(any());
        verify(settings, never()).getUploadLimits();
    }

    @Test
    void velicinaSeProvjeravaNakonLimitaBrojaSlika() {
        User artist = artist();
        UUID artworkId = UUID.randomUUID();
        var file = new MockMultipartFile("file", "velika.jpg", "image/jpeg", new byte[] {1});
        when(currentUserProvider.getCurrentUser()).thenReturn(artist);
        when(artworks.findByIdForUpdate(artworkId)).thenReturn(Optional.of(artwork(artist)));
        when(settings.getUploadLimits()).thenReturn(limits(3));
        when(artworkImages.countByArtwork_Id(artworkId)).thenReturn(0L);
        when(fileStorage.store(artist.getId(), FilePurpose.ARTWORK_IMAGE, file,
                Set.of("image/jpeg", "image/png"), 5242880)).thenThrow(new FileTooLargeException());

        assertThatThrownBy(() -> service().uploadImage(artworkId, file, null))
                .isInstanceOf(FileTooLargeException.class);
        var inOrder = org.mockito.Mockito.inOrder(artworkImages, fileStorage);
        inOrder.verify(artworkImages).countByArtwork_Id(artworkId);
        inOrder.verify(fileStorage).store(artist.getId(), FilePurpose.ARTWORK_IMAGE, file,
                Set.of("image/jpeg", "image/png"), 5242880);
        verify(artworkImages, never()).findMaxPositionByArtworkId(any());
    }

    @Test
    void vrstaSeProvjeravaNakonBrojaSlikaIVelicine() {
        User artist = artist();
        UUID artworkId = UUID.randomUUID();
        var file = new MockMultipartFile("file", "tekst.txt", "text/plain", new byte[] {1});
        when(currentUserProvider.getCurrentUser()).thenReturn(artist);
        when(artworks.findByIdForUpdate(artworkId)).thenReturn(Optional.of(artwork(artist)));
        when(settings.getUploadLimits()).thenReturn(limits(3));
        when(artworkImages.countByArtwork_Id(artworkId)).thenReturn(0L);
        when(fileStorage.store(artist.getId(), FilePurpose.ARTWORK_IMAGE, file,
                Set.of("image/jpeg", "image/png"), 5242880)).thenThrow(new UnsupportedFileTypeException());

        assertThatThrownBy(() -> service().uploadImage(artworkId, file, null))
                .isInstanceOf(UnsupportedFileTypeException.class);
        var inOrder = org.mockito.Mockito.inOrder(artworkImages, fileStorage);
        inOrder.verify(artworkImages).countByArtwork_Id(artworkId);
        inOrder.verify(fileStorage).store(artist.getId(), FilePurpose.ARTWORK_IMAGE, file,
                Set.of("image/jpeg", "image/png"), 5242880);
        verify(artworkImages, never()).findMaxPositionByArtworkId(any());
    }

    private ArtworkService service() {
        return new ArtworkService(artworks, artworkImages, users,
                currentUserProvider, artworkMapper, visibilityPolicy, settings, fileStorage);
    }

    private static Artwork artwork(User artist) {
        return Artwork.create(artist, "Morska tišina", (short) 2024, "Ulje", null, null,
                ArtworkVisibility.PUBLIC);
    }

    private static UploadLimits limits(int maxImages) {
        return new UploadLimits(5242880, maxImages, 5242880, 10485760, 10);
    }

    private static User artist() {
        return TestUsers.user(ARTIST_ID, "google-umjetnik", "umjetnik@artconnect.test",
                hr.fer.artconnect.auth.domain.UserRole.ARTIST,
                hr.fer.artconnect.auth.domain.UserStatus.ACTIVE);
    }
}
