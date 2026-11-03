package hr.fer.artconnect.artwork.service;

import hr.fer.artconnect.artwork.domain.Artwork;
import hr.fer.artconnect.artwork.domain.ArtworkImage;
import hr.fer.artconnect.artwork.domain.ArtworkImageRepository;
import hr.fer.artconnect.artwork.domain.ArtworkRepository;
import hr.fer.artconnect.artwork.domain.ArtworkVisibility;
import hr.fer.artconnect.artwork.web.ArtworkDto;
import hr.fer.artconnect.artwork.web.ArtworkImageDto;
import hr.fer.artconnect.artwork.web.ArtworkRequest;
import hr.fer.artconnect.auth.CurrentUserProvider;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRepository;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import hr.fer.artconnect.common.PageRequestParams;
import hr.fer.artconnect.common.PageResponse;
import hr.fer.artconnect.catalog.service.SettingsService;
import hr.fer.artconnect.file.domain.FilePurpose;
import hr.fer.artconnect.file.service.FileStorageService;
import hr.fer.artconnect.file.service.StoredFileDto;
import java.util.Set;
import org.springframework.web.multipart.MultipartFile;
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
    private final SettingsService settings;
    private final FileStorageService fileStorage;

    public ArtworkService(ArtworkRepository artworks, ArtworkImageRepository artworkImages, UserRepository users,
            CurrentUserProvider currentUserProvider, ArtworkMapper artworkMapper,
            ArtworkVisibilityPolicy visibilityPolicy, SettingsService settings, FileStorageService fileStorage) {
        this.artworks = artworks;
        this.artworkImages = artworkImages;
        this.users = users;
        this.currentUserProvider = currentUserProvider;
        this.artworkMapper = artworkMapper;
        this.visibilityPolicy = visibilityPolicy;
        this.settings = settings;
        this.fileStorage = fileStorage;
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

    @Transactional
    public ArtworkImageDto uploadImage(UUID artworkId, MultipartFile file, String altText) {
        User artist = currentUserProvider.getCurrentUser();
        if (artist.getStatus() == UserStatus.BLOCKED) {
            throw new ApiException(ErrorCode.USER_BLOCKED, "Korisnički račun je blokiran.");
        }
        Artwork artwork = artworks.findByIdForUpdate(artworkId).orElseThrow(ArtworkService::notFound);
        if (!artwork.getArtist().getId().equals(artist.getId())) {
            throw notFound();
        }

        var limits = settings.getUploadLimits();
        if (artworkImages.countByArtwork_Id(artworkId) >= limits.artworkMaxImages()) {
            throw new ApiException(ErrorCode.IMAGE_LIMIT_REACHED, "Djelo je doseglo dopušteni broj slika.");
        }
        if (altText != null && altText.length() > 300) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Alternativni tekst smije imati najviše 300 znakova.");
        }

        var storedFile = fileStorage.store(artist.getId(), FilePurpose.ARTWORK_IMAGE, file,
                Set.of("image/jpeg", "image/png"), limits.artworkMaxImageBytes());
        Short maxPosition = artworkImages.findMaxPositionByArtworkId(artworkId);
        short position = (short) (maxPosition == null ? 0 : maxPosition + 1);
        String normalizedAltText = altText == null || altText.isBlank() ? artwork.getTitle() : altText.strip();
        ArtworkImage savedImage = artworkImages.save(
                ArtworkImage.create(artwork, storedFile, normalizedAltText, position));
        artwork.markUpdated();
        return new ArtworkImageDto(savedImage.getId(), StoredFileDto.from(storedFile), savedImage.getAltText());
    }

    @Transactional
    public void deleteImage(UUID artworkId, UUID imageId) {
        User artist = currentUserProvider.getCurrentUser();
        if (artist.getStatus() == UserStatus.BLOCKED) {
            throw new ApiException(ErrorCode.USER_BLOCKED, "Korisnički račun je blokiran.");
        }
        Artwork artwork = artworks.findByIdForUpdate(artworkId).orElseThrow(ArtworkService::notFound);
        if (!artwork.getArtist().getId().equals(artist.getId())) {
            throw notFound();
        }

        ArtworkImage image = artworkImages.findByIdAndArtwork_Id(imageId, artworkId)
                .orElseThrow(ArtworkService::notFound);
        artworkImages.delete(image);
        artworkImages.flush();
        fileStorage.delete(image.getFile());
        artwork.markUpdated();
    }

    @Transactional
    public ArtworkDto update(UUID artworkId, ArtworkRequest request) {
        User artist = currentUserProvider.getCurrentUser();
        if (artist.getStatus() == UserStatus.BLOCKED) {
            throw new ApiException(ErrorCode.USER_BLOCKED, "Korisnički račun je blokiran.");
        }
        Artwork artwork = artworks.findById(artworkId).orElseThrow(ArtworkService::notFound);
        if (!artwork.getArtist().getId().equals(artist.getId())) {
            throw notFound();
        }
        artwork.updateDetails(
                request.title(),
                request.year().shortValue(),
                request.technique(),
                request.dimensions(),
                request.description(),
                request.visibility());
        Artwork saved = artworks.save(artwork);
        Map<UUID, List<ArtworkImage>> imagesByArtwork = loadImages(List.of(saved));
        return artworkMapper.toDto(saved, imagesByArtwork.getOrDefault(artworkId, List.of()));
    }

    @Transactional
    public void delete(UUID artworkId) {
        User artist = currentUserProvider.getCurrentUser();
        if (artist.getStatus() == UserStatus.BLOCKED) {
            throw new ApiException(ErrorCode.USER_BLOCKED, "Korisnički račun je blokiran.");
        }
        Artwork artwork = artworks.findById(artworkId).orElseThrow(ArtworkService::notFound);
        if (!artwork.getArtist().getId().equals(artist.getId())) {
            throw notFound();
        }

        List<ArtworkImage> images = artworkImages.findAllWithFileByArtworkIds(List.of(artworkId));
        artworkImages.deleteAll(images);
        artworkImages.flush();
        images.forEach(image -> fileStorage.delete(image.getFile()));
        artworks.delete(artwork);
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
