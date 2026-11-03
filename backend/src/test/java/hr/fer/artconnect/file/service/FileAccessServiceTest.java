package hr.fer.artconnect.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hr.fer.artconnect.artwork.domain.Artwork;
import hr.fer.artconnect.artwork.domain.ArtworkImage;
import hr.fer.artconnect.artwork.domain.ArtworkImageRepository;
import hr.fer.artconnect.artwork.domain.ArtworkVisibility;
import hr.fer.artconnect.artwork.service.ArtworkVisibilityPolicy;
import hr.fer.artconnect.auth.CurrentUserProvider;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import hr.fer.artconnect.file.domain.FilePurpose;
import hr.fer.artconnect.file.domain.StoredFile;
import hr.fer.artconnect.file.domain.StoredFileRepository;
import hr.fer.artconnect.support.TestUsers;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.util.ReflectionTestUtils;

class FileAccessServiceTest {
    private static final byte[] CONTENT = {1, 2, 3};
    private static final User ARTIST = artist(UserStatus.ACTIVE);
    private static final User OTHER_ARTIST =
            TestUsers.user(UUID.randomUUID(), "sub-other", "drugi@artconnect.test",
                    UserRole.ARTIST, UserStatus.ACTIVE);
    private static final User ADMIN =
            TestUsers.user(UUID.randomUUID(), "sub-admin", "admin@artconnect.test",
                    UserRole.ADMIN, UserStatus.ACTIVE);

    private StoredFileRepository storedFiles;
    private ArtworkImageRepository artworkImages;
    private CurrentUserProvider currentUserProvider;
    private FileStorageService fileStorage;
    private FileAccessService service;

    @BeforeEach
    void setUp() {
        storedFiles = mock(StoredFileRepository.class);
        artworkImages = mock(ArtworkImageRepository.class);
        currentUserProvider = mock(CurrentUserProvider.class);
        fileStorage = mock(FileStorageService.class);
        service = new FileAccessService(storedFiles, artworkImages, new ArtworkVisibilityPolicy(),
                currentUserProvider, fileStorage);
    }

    @Test
    void sliku_javnogDjelaVideSviIKesiraSeJavno() {
        StoredFile file = imageOf(ArtworkVisibility.PUBLIC, false, UserStatus.ACTIVE);

        for (User viewer : new User[] {null, OTHER_ARTIST, ARTIST, ADMIN}) {
            viewAs(viewer);
            FileDownload download = service.resolve(file.getId());

            assertThat(download.contentType()).isEqualTo("image/png");
            assertThat(download.sizeBytes()).isEqualTo(CONTENT.length);
            assertThat(download.originalName()).isEqualTo("crtež.png");
            assertThat(download.publiclyCacheable()).isTrue();
        }
    }

    @Test
    void sliku_privatnogSkrivenogIliBlokiranogDjelaVideSamoVlasnikIAdministrator() {
        StoredFile privateImage = imageOf(ArtworkVisibility.PRIVATE, false, UserStatus.ACTIVE);
        StoredFile hiddenImage = imageOf(ArtworkVisibility.PUBLIC, true, UserStatus.ACTIVE);
        StoredFile blockedImage = imageOf(ArtworkVisibility.PUBLIC, false, UserStatus.BLOCKED);

        for (StoredFile file : new StoredFile[] {privateImage, hiddenImage, blockedImage}) {
            for (User viewer : new User[] {null, OTHER_ARTIST}) {
                viewAs(viewer);
                // Postojanje skrivene datoteke se ne otkriva, pa je i ovo 404, a ne 403.
                assertFails(file, ErrorCode.NOT_FOUND);
            }
            for (User viewer : new User[] {file.getOwner(), ADMIN}) {
                viewAs(viewer);
                assertThat(service.resolve(file.getId()).publiclyCacheable()).isFalse();
            }
        }
    }

    @Test
    void prilogPrijaveVideSamoVlasnikIAdministratorOstaliDobivajuForbidden() {
        StoredFile attachment = storedFile(FilePurpose.APPLICATION_ATTACHMENT, "zivotopis.pdf",
                "application/pdf", "application_attachment/" + UUID.randomUUID() + ".pdf");
        onDisk(attachment);

        for (User viewer : new User[] {null, OTHER_ARTIST}) {
            viewAs(viewer);
            assertFails(attachment, ErrorCode.FORBIDDEN);
        }

        for (User viewer : new User[] {ARTIST, ADMIN}) {
            viewAs(viewer);
            FileDownload download = service.resolve(attachment.getId());
            assertThat(download.contentType()).isEqualTo("application/pdf");
            assertThat(download.publiclyCacheable()).isFalse();
        }
    }

    @Test
    void nepostojecaDatotekaIOnaBezVezanogDjelaVracaju404() {
        UUID unknownId = UUID.randomUUID();
        when(storedFiles.findById(unknownId)).thenReturn(Optional.empty());
        StoredFile unlinked = storedFile(FilePurpose.ARTWORK_IMAGE, "nevezana.png", "image/png",
                "artwork_image/" + UUID.randomUUID() + ".png");
        onDisk(unlinked);
        when(artworkImages.findWithArtworkByFileId(unlinked.getId())).thenReturn(Optional.empty());

        viewAs(null);
        assertThatThrownBy(() -> service.resolve(unknownId))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        assertFails(unlinked, ErrorCode.NOT_FOUND);

        // Slika koja još nije vezana na djelo dostupna je vlasniku.
        viewAs(ARTIST);
        assertThat(service.resolve(unlinked.getId()).publiclyCacheable()).isFalse();
    }

    @Test
    void datotekaKojeNemaNaDiskuVraca404IakoJeDostupnaPoPravima() {
        StoredFile file = imageOf(ArtworkVisibility.PUBLIC, false, UserStatus.ACTIVE);
        when(fileStorage.load(file))
                .thenReturn(new FileSystemResource(Path.of("target", "nema-ove-datoteke.png")));

        viewAs(null);
        assertFails(file, ErrorCode.NOT_FOUND);
    }

    private void assertFails(StoredFile file, ErrorCode expected) {
        assertThatThrownBy(() -> service.resolve(file.getId()))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(expected);
    }

    private void viewAs(User viewer) {
        when(currentUserProvider.getOptionalCurrentUser()).thenReturn(Optional.ofNullable(viewer));
    }

    /** Slika vezana na djelo zadane vidljivosti, čiji autor ima zadani status. */
    private StoredFile imageOf(ArtworkVisibility visibility, boolean hidden, UserStatus artistStatus) {
        User artist = artistStatus == UserStatus.ACTIVE ? ARTIST : artist(artistStatus);
        StoredFile file = storedFile(FilePurpose.ARTWORK_IMAGE, "crtež.png", "image/png",
                "artwork_image/" + UUID.randomUUID() + ".png", artist);
        Artwork artwork = Artwork.create(artist, "Djelo", (short) 2024, "Ulje", null, null, visibility);
        ReflectionTestUtils.setField(artwork, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(artwork, "hiddenByAdmin", hidden);
        ArtworkImage image = BeanUtils.instantiateClass(ArtworkImage.class);
        ReflectionTestUtils.setField(image, "artwork", artwork);
        ReflectionTestUtils.setField(image, "file", file);
        when(artworkImages.findWithArtworkByFileId(file.getId())).thenReturn(Optional.of(image));
        onDisk(file);
        return file;
    }

    private StoredFile storedFile(FilePurpose purpose, String originalName, String contentType,
            String storageKey) {
        return storedFile(purpose, originalName, contentType, storageKey, ARTIST);
    }

    private StoredFile storedFile(FilePurpose purpose, String originalName, String contentType,
            String storageKey, User owner) {
        StoredFile file = new StoredFile(owner, purpose, originalName, contentType,
                CONTENT.length, storageKey);
        ReflectionTestUtils.setField(file, "id", UUID.randomUUID());
        when(storedFiles.findById(file.getId())).thenReturn(Optional.of(file));
        return file;
    }

    private void onDisk(StoredFile file) {
        when(fileStorage.load(file)).thenReturn(new ByteArrayResource(CONTENT));
    }

    private static User artist(UserStatus status) {
        return TestUsers.user(UUID.randomUUID(), "sub-artist", "umjetnik@artconnect.test",
                UserRole.ARTIST, status);
    }
}
