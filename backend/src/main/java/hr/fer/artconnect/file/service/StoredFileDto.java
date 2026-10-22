package hr.fer.artconnect.file.service;

import hr.fer.artconnect.file.domain.StoredFile;
import java.util.UUID;

public record StoredFileDto(UUID id, String originalName, String contentType, long sizeBytes, String url) {
    public static StoredFileDto from(StoredFile file) {
        return new StoredFileDto(file.getId(), file.getOriginalName(), file.getContentType(),
                file.getSizeBytes(), "/api/v1/files/" + file.getId());
    }
}
