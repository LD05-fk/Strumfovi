package hr.fer.artconnect.file.service;

import static org.assertj.core.api.Assertions.assertThat;

import hr.fer.artconnect.file.domain.FilePurpose;
import hr.fer.artconnect.file.domain.StoredFile;
import hr.fer.artconnect.file.domain.StoredFileRepository;
import hr.fer.artconnect.support.IntegrationTestBase;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(properties = "artconnect.storage.root=target/test-uploads")
class FileStorageIntegrationTest extends IntegrationTestBase {
    private static final byte[] JPEG = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00};
    private final UUID ownerId = UUID.randomUUID();

    @Autowired
    FileStorageService storage;

    @Autowired
    StoredFileRepository storedFiles;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transactionManager;

    @BeforeEach
    void createOwner() {
        jdbc.update("INSERT INTO users (id, display_name, created_at) VALUES (?, ?, ?)",
                ownerId, "Vlasnik datoteke", OffsetDateTime.now());
    }

    @Test
    void rollbackNakonZapisaUklanjaDatotekuIDatabaseRed() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        StoredFile stored = transaction.execute(status -> {
            StoredFile file = storage.store(ownerId, FilePurpose.ARTWORK_IMAGE,
                    multipart(), Set.of("image/jpeg"), 1024);
            status.setRollbackOnly();
            return file;
        });

        assertThat(stored).isNotNull();
        assertThat(filePath(stored)).doesNotExist();
        assertThat(storedFiles.count()).isZero();
    }

    @Test
    void deleteBriseDatotekuTekNakonCommita() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        StoredFile stored = transaction.execute(status -> storage.store(ownerId,
                FilePurpose.ARTWORK_IMAGE, multipart(), Set.of("image/jpeg"), 1024));
        Path path = filePath(stored);

        transaction.executeWithoutResult(status -> storage.delete(stored));

        assertThat(path).doesNotExist();
        assertThat(storedFiles.findById(stored.getId())).isEmpty();
    }

    private MockMultipartFile multipart() {
        return new MockMultipartFile("file", "slika.jpg", "text/plain", JPEG);
    }

    private Path filePath(StoredFile stored) {
        return Path.of("target/test-uploads").toAbsolutePath().normalize().resolve(stored.getStorageKey());
    }
}
