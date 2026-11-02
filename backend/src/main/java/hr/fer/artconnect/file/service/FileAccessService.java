package hr.fer.artconnect.file.service;

import hr.fer.artconnect.artwork.domain.Artwork;
import hr.fer.artconnect.artwork.domain.ArtworkImage;
import hr.fer.artconnect.artwork.domain.ArtworkImageRepository;
import hr.fer.artconnect.artwork.domain.ArtworkVisibility;
import hr.fer.artconnect.artwork.service.ArtworkVisibilityPolicy;
import hr.fer.artconnect.auth.CurrentUserProvider;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import hr.fer.artconnect.file.domain.StoredFile;
import hr.fer.artconnect.file.domain.StoredFileRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class FileAccessService {
    private static final Logger log = LoggerFactory.getLogger(FileAccessService.class);

    private final StoredFileRepository storedFiles;
    private final ArtworkImageRepository artworkImages;
    private final ArtworkVisibilityPolicy visibilityPolicy;
    private final CurrentUserProvider currentUserProvider;
    private final FileStorageService fileStorage;

    public FileAccessService(StoredFileRepository storedFiles, ArtworkImageRepository artworkImages,
            ArtworkVisibilityPolicy visibilityPolicy, CurrentUserProvider currentUserProvider,
            FileStorageService fileStorage) {
        this.storedFiles = storedFiles;
        this.artworkImages = artworkImages;
        this.visibilityPolicy = visibilityPolicy;
        this.currentUserProvider = currentUserProvider;
        this.fileStorage = fileStorage;
    }

    @Transactional(readOnly = true)
    public FileDownload resolve(UUID fileId) {
        StoredFile file = storedFiles.findById(fileId).orElseThrow(FileAccessService::notFound);
        User viewer = currentUserProvider.getOptionalCurrentUser().orElse(null);

        boolean publiclyCacheable = switch (file.getPurpose()) {
            case ARTWORK_IMAGE -> resolveArtworkImageAccess(file, viewer);
            case APPLICATION_ATTACHMENT -> resolveAttachmentAccess(file, viewer);
        };

        Resource resource = fileStorage.load(file);
        if (!resource.exists() || !resource.isReadable()) {
            // Zapis u bazi bez datoteke na disku je greška pohrane, a korisniku ostaje 404.
            log.error("Datoteka {} zapisana je u bazi, ali nije dostupna na disku: {}",
                    file.getId(), file.getStorageKey());
            throw notFound();
        }

        return new FileDownload(resource, file.getContentType(), file.getSizeBytes(),
                file.getOriginalName(), publiclyCacheable);
    }

    private boolean resolveArtworkImageAccess(StoredFile file, User viewer) {
        Artwork artwork = artworkImages.findWithArtworkByFileId(file.getId())
                .map(ArtworkImage::getArtwork)
                .orElse(null);
        if (artwork == null) {
            // Učitana slika koja još nije vezana na djelo, vidi ju samo vlasnik i administrator.
            if (!isOwnerOrAdministrator(file, viewer)) {
                throw notFound();
            }
            return false;
        }
        if (!visibilityPolicy.canViewArtwork(artwork, viewer)) {
            throw notFound();
        }
        return isPublicArtwork(artwork);
    }

    /** Prilog prijave: u Fazi 1 samo vlasnik i administrator, organizatora dodaje APP-04-BE3. */
    private boolean resolveAttachmentAccess(StoredFile file, User viewer) {
        if (!isOwnerOrAdministrator(file, viewer)) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Nemate pravo pristupa ovoj datoteci.");
        }
        return false;
    }

    private static boolean isPublicArtwork(Artwork artwork) {
        return artwork.getVisibility() == ArtworkVisibility.PUBLIC
                && !artwork.isHiddenByAdmin()
                && artwork.getArtist().getStatus() == UserStatus.ACTIVE;
    }

    private static boolean isOwnerOrAdministrator(StoredFile file, User viewer) {
        return viewer != null && (file.getOwner().getId().equals(viewer.getId())
                || viewer.getRole() == UserRole.ADMIN);
    }

    private static ApiException notFound() {
        return new ApiException(ErrorCode.NOT_FOUND, "Datoteka nije pronađena.");
    }
}
