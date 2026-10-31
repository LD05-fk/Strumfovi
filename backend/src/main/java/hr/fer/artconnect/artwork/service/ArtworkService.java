package hr.fer.artconnect.artwork.service;

import hr.fer.artconnect.artwork.domain.Artwork;
import hr.fer.artconnect.artwork.domain.ArtworkImage;
import hr.fer.artconnect.artwork.domain.ArtworkImageRepository;
import hr.fer.artconnect.artwork.domain.ArtworkRepository;
import hr.fer.artconnect.artwork.domain.ArtworkVisibility;
import hr.fer.artconnect.artwork.web.ArtworkDto;
import hr.fer.artconnect.artwork.web.ArtworkRequest;
import hr.fer.artconnect.auth.CurrentUserProvider;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRepository;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import hr.fer.artconnect.common.PageRequestParams;
import hr.fer.artconnect.common.PageResponse;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArtworkService {
    private final ArtworkRepository artworks;
    private final ArtworkImageRepository artworkImages;
    private final UserRepository users;
    private final CurrentUserProvider currentUserProvider;
    private final ArtworkMapper artworkMapper;
    private final ArtworkVisibilityPolicy visibilityPolicy;

    public ArtworkService(ArtworkRepository artworks, ArtworkImageRepository artworkImages, UserRepository users,
            CurrentUserProvider currentUserProvider, ArtworkMapper artworkMapper,
            ArtworkVisibilityPolicy visibilityPolicy) {
        this.artworks = artworks;
        this.artworkImages = artworkImages;
        this.users = users;
        this.currentUserProvider = currentUserProvider;
        this.artworkMapper = artworkMapper;
        this.visibilityPolicy = visibilityPolicy;
    }

    @Transactional
    public ArtworkDto create(ArtworkRequest request) {
        var artist = currentUserProvider.getCurrentUser();
        if (artist.getStatus() == UserStatus.BLOCKED) {
            throw new ApiException(ErrorCode.USER_BLOCKED, "Korisnički račun je blokiran.");
        }
        Artwork saved = artworks.save(Artwork.create(
                artist,
                request.title(),
                request.year().shortValue(),
                request.technique(),
                request.dimensions(),
                request.description(),
                request.visibility()));
        return artworkMapper.toDto(saved, List.of());
    }

    @Transactional(readOnly = true)
    public PageResponse<ArtworkDto> listArtistArtworks(UUID artistId, PageRequestParams params) {
        User artist = users.findById(artistId).orElseThrow(ArtworkService::notFound);
        User viewer = currentUserProvider.getOptionalCurrentUser().orElse(null);
        if (!visibilityPolicy.canViewArtist(artist, viewer)) {
            throw notFound();
        }

        var pageable = params.toPageable(Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Artwork> page = visibilityPolicy.canViewAllArtworks(artist, viewer)
                ? artworks.findAllByArtist_Id(artistId, pageable)
                : artworks.findAllByArtist_IdAndVisibilityAndHiddenByAdminFalse(
                        artistId, ArtworkVisibility.PUBLIC, pageable);
        Map<UUID, List<ArtworkImage>> imagesByArtwork = loadImages(page.getContent());
        return PageResponse.from(page.map(artwork ->
                artworkMapper.toDto(artwork, imagesByArtwork.getOrDefault(artwork.getId(), List.of()))));
    }

    @Transactional(readOnly = true)
    public ArtworkDto getArtwork(UUID artworkId) {
        Artwork artwork = artworks.findById(artworkId).orElseThrow(ArtworkService::notFound);
        User viewer = currentUserProvider.getOptionalCurrentUser().orElse(null);
        if (!visibilityPolicy.canViewArtwork(artwork, viewer)) {
            throw notFound();
        }
        Map<UUID, List<ArtworkImage>> imagesByArtwork = loadImages(List.of(artwork));
        return artworkMapper.toDto(artwork, imagesByArtwork.getOrDefault(artworkId, List.of()));
    }

    private Map<UUID, List<ArtworkImage>> loadImages(List<Artwork> page) {
        if (page.isEmpty()) {
            return Map.of();
        }
        List<UUID> artworkIds = page.stream().map(Artwork::getId).toList();
        return artworkImages.findAllWithFileByArtworkIds(artworkIds).stream()
                .collect(Collectors.groupingBy(image -> image.getArtwork().getId()));
    }

    private static ApiException notFound() {
        return new ApiException(ErrorCode.NOT_FOUND, "Traženo djelo ili portfolio nije pronađen.");
    }
}
