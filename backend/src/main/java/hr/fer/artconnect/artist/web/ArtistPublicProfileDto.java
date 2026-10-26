package hr.fer.artconnect.artist.web;

import java.util.List;
import java.util.UUID;

/** Odgovor javnog profila umjetnika. */
public record ArtistPublicProfileDto(
        UUID userId,
        String displayName,
        String avatarUrl,
        String biography,
        String artistStatement,
        List<ArtistProfileDto.CatalogRef> artFields,
        List<ArtistProfileDto.ArtistLink> links,
        List<PastExhibitionDto> pastExhibitions,
        List<Object> participations) {

    public ArtistPublicProfileDto {
        artFields = List.copyOf(artFields);
        links = List.copyOf(links);
        pastExhibitions = List.copyOf(pastExhibitions);
        participations = List.copyOf(participations);
    }
}
