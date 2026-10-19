package hr.fer.artconnect.catalog.web;

public record UploadLimits(long artworkMaxImageBytes, int artworkMaxImages,
        long applicationMaxImageBytes, long applicationMaxPdfBytes, int applicationMaxFiles) {
}
