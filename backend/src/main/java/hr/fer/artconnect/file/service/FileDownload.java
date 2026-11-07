package hr.fer.artconnect.file.service;

import org.springframework.core.io.Resource;


public record FileDownload(
        Resource resource,
        String contentType,
        long sizeBytes,
        String originalName,
        boolean publiclyCacheable) {
}
