package hr.fer.artconnect.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRepository;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import hr.fer.artconnect.file.domain.FilePurpose;
import hr.fer.artconnect.file.domain.StoredFile;
import hr.fer.artconnect.file.domain.StoredFileRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class FileStorageServiceTest {
    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final byte[] JPEG = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00};
    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00
    };
    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-', '1', '.', '7'};

    @TempDir
    Path tempDir;

    private StoredFileRepository storedFiles;
    private UserRepository users;
    private FileStorageService service;

    @BeforeEach
    void setUp() {
        storedFiles = mock(StoredFileRepository.class);
        users = mock(UserRepository.class);
        when(users.getReferenceById(OWNER_ID)).thenReturn(mock(User.class));
        when(storedFiles.save(any(StoredFile.class))).thenAnswer(invocation -> {
            StoredFile saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", UUID.randomUUID());
            return saved;
        });
        FileStorageProperties properties = new FileStorageProperties();
        properties.setRoot(tempDir);
        service = new FileStorageService(properties, storedFiles, users);
        TransactionSynchronizationManager.initSynchronization();
    }

    @Test
    void konfiguracijaStvaraKorijenPohraneIProvjeravaZapisivost() throws Exception {
        Path root = tempDir.resolve("nested/uploads");
        FileStorageProperties properties = new FileStorageProperties();
        properties.setRoot(root);

        properties.initialize();

        assertThat(root).isDirectory();
        Path probe = Files.createTempFile(root, "write-", ".tmp");
        Files.delete(probe);
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void spremaJpegPngIPdfKadSuTipoviDopusteni() throws Exception {
        StoredFile jpeg = store("slika.jpg", JPEG, Set.of("image/jpeg"), 100);
        StoredFile png = store("slika.png", PNG, Set.of("image/png"), 100);
        StoredFile pdf = store("dokument.pdf", PDF, Set.of("application/pdf"), 100);

        assertThat(jpeg.getStorageKey()).matches("artwork_image/[0-9a-f-]{36}\\.jpg");
        assertThat(jpeg.getContentType()).isEqualTo("image/jpeg");
        assertThat(png.getStorageKey()).endsWith(".png");
        assertThat(png.getContentType()).isEqualTo("image/png");
        assertThat(pdf.getStorageKey()).endsWith(".pdf");
        assertThat(pdf.getContentType()).isEqualTo("application/pdf");
    }

    @Test
    void tipSadrzajaOdredujeEkstenzijuNeovisnoOPocetnomNazivu() throws Exception {
        StoredFile png = store("C:\\privatno\\slika.pdf", PNG, Set.of("image/png"), 100);

        assertThat(png.getOriginalName()).isEqualTo("slika.pdf");
        assertThat(png.getStorageKey()).endsWith(".png");
    }

    @Test
    void odbijaTekstPreimenovanUSliku() {
        assertThatThrownBy(() -> store("slika.jpg", "nije slika".getBytes(),
                Set.of("image/jpeg"), 100))
                .isInstanceOf(UnsupportedFileTypeException.class)
                .extracting(error -> ((ApiException) error).getCode())
                .isEqualTo(ErrorCode.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    void praznaDatotekaJeValidacijskaGreska() {
        assertThatThrownBy(() -> store("prazno.jpg", new byte[0], Set.of("image/jpeg"), 100))
                .isInstanceOf(ApiException.class)
                .extracting(error -> ((ApiException) error).getCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void datotekaVecaOdGraniceJe413() {
        assertThatThrownBy(() -> store("slika.jpg", JPEG, Set.of("image/jpeg"), JPEG.length - 1))
                .isInstanceOf(FileTooLargeException.class)
                .extracting(error -> ((ApiException) error).getCode())
                .isEqualTo(ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    void putanjaKojaIzlaziIzKorijenaSeOdbija() {
        StoredFile malicious = new StoredFile(mock(User.class), FilePurpose.ARTWORK_IMAGE,
                "x.jpg", "image/jpeg", 1, "../outside.jpg");

        assertThatThrownBy(() -> service.load(malicious))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("escapes");
    }

    @Test
    void rollbackBriseDatotekuKojaJeVecPremjestena() throws Exception {
        StoredFile stored = store("slika.jpg", JPEG, Set.of("image/jpeg"), 100);
        Path path = tempDir.resolve(stored.getStorageKey());
        assertThat(path).exists();

        synchronizations().forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(path).doesNotExist();
    }

    @Test
    void brisanjeDatotekeSeOdgadaDoCommita() throws Exception {
        StoredFile stored = store("slika.jpg", JPEG, Set.of("image/jpeg"), 100);
        Path path = tempDir.resolve(stored.getStorageKey());

        service.delete(stored);
        assertThat(path).exists();
        synchronizations().forEach(TransactionSynchronization::afterCommit);
        assertThat(path).doesNotExist();
    }

    private StoredFile store(String name, byte[] content, Set<String> allowedTypes, long maxBytes)
            throws IOException {
        return service.store(OWNER_ID, FilePurpose.ARTWORK_IMAGE,
                new MockMultipartFile("file", name, "text/plain", content), allowedTypes, maxBytes);
    }

    private java.util.List<TransactionSynchronization> synchronizations() {
        return TransactionSynchronizationManager.getSynchronizations();
    }
}
