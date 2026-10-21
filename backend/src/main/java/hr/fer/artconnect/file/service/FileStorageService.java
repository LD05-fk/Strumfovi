package hr.fer.artconnect.file.service;

import hr.fer.artconnect.auth.domain.UserRepository;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import hr.fer.artconnect.file.domain.FilePurpose;
import hr.fer.artconnect.file.domain.StoredFile;
import hr.fer.artconnect.file.domain.StoredFileRepository;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final int SIGNATURE_BYTES = 8;
    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "image/jpeg", "image/png", "application/pdf");

    private final Path root;
    private final StoredFileRepository storedFiles;
    private final UserRepository users;

    public FileStorageService(FileStorageProperties properties, StoredFileRepository storedFiles,
            UserRepository users) {
        this.root = properties.getRoot().toAbsolutePath().normalize();
        this.storedFiles = storedFiles;
        this.users = users;
    }

    @Transactional
    public StoredFile store(UUID ownerId, FilePurpose purpose, MultipartFile file,
            Set<String> allowedTypes, long maxBytes) {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(purpose, "purpose");
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(allowedTypes, "allowedTypes");
        if (file.getSize() <= 0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Datoteka ne smije biti prazna.");
        }
        if (file.getSize() > maxBytes) {
            throw new FileTooLargeException();
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("File storage requires an active transaction.");
        }

        Path temporary = null;
        Path destination = null;
        boolean transactionCleanupRegistered = false;
        try {
            temporary = Files.createTempFile(root, ".upload-", ".tmp");
            try (InputStream input = file.getInputStream()) {
                copyWithinLimit(input, temporary, maxBytes);
            }
            long actualSize = Files.size(temporary);
            if (actualSize <= 0) {
                throw new ApiException(ErrorCode.VALIDATION_ERROR, "Datoteka ne smije biti prazna.");
            }
            if (actualSize > maxBytes) {
                throw new FileTooLargeException();
            }

            String contentType = detectContentType(temporary);
            if (contentType == null || !SUPPORTED_TYPES.contains(contentType)
                    || !allowedTypes.contains(contentType)) {
                throw new UnsupportedFileTypeException();
            }

            String extension = extensionFor(contentType);
            String storageKey = purpose.name().toLowerCase(Locale.ROOT) + "/"
                    + UUID.randomUUID() + "." + extension;
            destination = resolveStorageKey(storageKey);
            Files.createDirectories(destination.getParent());
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
            temporary = null;

            Path committedFile = destination;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        deleteAfterRollback(committedFile);
                    }
                }
            });
            transactionCleanupRegistered = true;

            StoredFile storedFile = new StoredFile(users.getReferenceById(ownerId), purpose,
                    cleanOriginalName(file.getOriginalFilename()), contentType, actualSize, storageKey);
            return storedFiles.save(storedFile);
        } catch (IOException exception) {
            deleteQuietly(temporary);
            if (!transactionCleanupRegistered) {
                deleteQuietly(destination);
            }
            throw new IllegalStateException("Could not store uploaded file.", exception);
        } catch (RuntimeException exception) {
            deleteQuietly(temporary);
            if (!transactionCleanupRegistered) {
                deleteQuietly(destination);
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public Resource load(StoredFile storedFile) {
        Path path = resolveStorageKey(storedFile.getStorageKey());
        try {
            return new UrlResource(path.toUri());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load stored file.", exception);
        }
    }

    @Transactional
    public void delete(StoredFile storedFile) {
        Path path = resolveStorageKey(storedFile.getStorageKey());
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("File deletion requires an active transaction.");
        }
        storedFiles.delete(storedFile);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    if (!Files.deleteIfExists(path)) {
                        log.warn("Stored file is missing after metadata deletion: {}", path);
                    }
                } catch (IOException exception) {
                    log.warn("Could not delete stored file after metadata deletion: {}", path, exception);
                }
            }
        });
    }

    private Path resolveStorageKey(String storageKey) {
        Path path = root.resolve(storageKey).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalStateException("Stored file path escapes the storage root.");
        }
        return path;
    }

    private String detectContentType(Path path) throws IOException {
        byte[] signature;
        try (InputStream input = Files.newInputStream(path)) {
            signature = input.readNBytes(SIGNATURE_BYTES);
        }
        if (signature.length >= 3 && (signature[0] & 0xff) == 0xff
                && (signature[1] & 0xff) == 0xd8 && (signature[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        if (signature.length == SIGNATURE_BYTES
                && (signature[0] & 0xff) == 0x89
                && signature[1] == 0x50 && signature[2] == 0x4e && signature[3] == 0x47
                && signature[4] == 0x0d && signature[5] == 0x0a
                && signature[6] == 0x1a && signature[7] == 0x0a) {
            return "image/png";
        }
        if (signature.length >= 5 && signature[0] == 0x25
                && signature[1] == 0x50 && signature[2] == 0x44
                && signature[3] == 0x46 && signature[4] == 0x2d) {
            return "application/pdf";
        }
        return null;
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "application/pdf" -> "pdf";
            default -> throw new UnsupportedFileTypeException();
        };
    }

    private void copyWithinLimit(InputStream input, Path target, long maxBytes) throws IOException {
        try (OutputStream output = Files.newOutputStream(target)) {
            byte[] buffer = new byte[8192];
            long copied = 0;
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (read > maxBytes - copied) {
                    throw new FileTooLargeException();
                }
                output.write(buffer, 0, read);
                copied += read;
            }
        }
    }

    private String cleanOriginalName(String originalName) {
        if (originalName == null) {
            return "file";
        }
        int lastSeparator = Math.max(originalName.lastIndexOf('/'), originalName.lastIndexOf('\\'));
        String name = originalName.substring(lastSeparator + 1);
        if (name.isBlank()) {
            return "file";
        }
        return name.codePoints().limit(255)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private void deleteAfterRollback(Path path) {
        try {
            if (!Files.deleteIfExists(path)) {
                log.warn("Stored file is missing after transaction rollback: {}", path);
            }
        } catch (IOException exception) {
            log.warn("Could not delete stored file after transaction rollback: {}", path, exception);
        }
    }

    private void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            log.warn("Could not clean up temporary stored file: {}", path, exception);
        }
    }
}
