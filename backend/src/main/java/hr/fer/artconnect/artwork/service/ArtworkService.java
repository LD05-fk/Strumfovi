package hr.fer.artconnect.artwork.service;

import hr.fer.artconnect.artwork.domain.Artwork;
import hr.fer.artconnect.artwork.domain.ArtworkRepository;
import hr.fer.artconnect.artwork.web.ArtworkDto;
import hr.fer.artconnect.artwork.web.ArtworkRequest;
import hr.fer.artconnect.auth.CurrentUserProvider;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArtworkService {
    private final ArtworkRepository artworks;
    private final CurrentUserProvider currentUserProvider;
    private final ArtworkMapper artworkMapper;

    public ArtworkService(ArtworkRepository artworks, CurrentUserProvider currentUserProvider,
            ArtworkMapper artworkMapper) {
        this.artworks = artworks;
        this.currentUserProvider = currentUserProvider;
        this.artworkMapper = artworkMapper;
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
}
