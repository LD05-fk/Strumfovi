package hr.fer.artconnect.artwork.web;

import hr.fer.artconnect.file.service.StoredFileDto;
import java.util.UUID;

public record ArtworkImageDto(UUID id, StoredFileDto file, String altText) {
}
