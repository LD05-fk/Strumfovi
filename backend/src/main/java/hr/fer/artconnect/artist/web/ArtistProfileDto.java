package hr.fer.artconnect.artist.web;

import java.util.List;
import java.util.UUID;

/** Odgovor GET /me/artist-profile. */
public record ArtistProfileDto(
        UUID userId,
        String displayName,
        String avatarUrl,
        String biography,
        String artistStatement,
        List<CatalogRef> artFields,
        List<ArtistLink> links) {

    public record CatalogRef(UUID id, String code, String name) {}

    public record ArtistLink(String label, String url) {}
}
