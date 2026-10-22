package hr.fer.artconnect.file.service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("artconnect.storage")
@Validated
public class FileStorageProperties {
    private Path root = Path.of("/data/uploads");

    public Path getRoot() {
        return root;
    }

    public void setRoot(Path root) {
        this.root = root;
    }

    @PostConstruct
    void initialize() {
        if (root == null) {
            throw new IllegalStateException("artconnect.storage.root must be configured.");
        }
        root = root.toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
            if (!Files.isDirectory(root)) {
                throw new IllegalStateException("Storage root is not a directory: " + root);
            }
            Path probe = Files.createTempFile(root, ".write-test-", ".tmp");
            Files.delete(probe);
        } catch (IOException exception) {
            throw new IllegalStateException("Storage root is not writable: " + root, exception);
        }
    }
}
