package hr.fer.artconnect.artist.service;

import hr.fer.artconnect.artist.domain.ArtistLink;
import hr.fer.artconnect.artist.domain.ArtistProfile;
import hr.fer.artconnect.artist.web.ArtistProfileDto;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.catalog.domain.ArtField;
import java.util.Comparator;
import java.util.List;

/** Mapira polja zajednička vlastitom i javnom profilu umjetnika. */
public final class ArtistProfileMapper {
    private ArtistProfileMapper() {}

    public static ArtistProfileDto toDto(User user, ArtistProfile profile, List<ArtistLink> links) {
        List<ArtistProfileDto.CatalogRef> artFields = profile == null
                ? List.of()
                : profile.getArtFields().stream()
                        .sorted(Comparator.comparingInt(ArtField::getPosition).thenComparing(ArtField::getId))
                        .map(field -> new ArtistProfileDto.CatalogRef(
                                field.getId(), field.getCode(), field.getName()))
                        .toList();
        List<ArtistProfileDto.ArtistLink> artistLinks = links.stream()
                .map(link -> new ArtistProfileDto.ArtistLink(link.getLabel(), link.getUrl()))
                .toList();

        return new ArtistProfileDto(
                user.getId(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                profile == null ? null : profile.getBiography(),
                profile == null ? null : profile.getArtistStatement(),
                artFields,
                artistLinks);
    }
}
