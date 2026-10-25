package hr.fer.artconnect.artist.service;

import hr.fer.artconnect.artist.domain.ArtistLinkRepository;
import hr.fer.artconnect.artist.domain.ArtistLink;
import hr.fer.artconnect.artist.domain.ArtistProfileRepository;
import hr.fer.artconnect.artist.web.ArtistProfileDto;
import hr.fer.artconnect.auth.CurrentUserProvider;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArtistProfileService {
    private final ArtistProfileRepository artistProfiles;
    private final ArtistLinkRepository artistLinks;
    private final CurrentUserProvider currentUserProvider;

    public ArtistProfileService(ArtistProfileRepository artistProfiles, ArtistLinkRepository artistLinks,
            CurrentUserProvider currentUserProvider) {
        this.artistProfiles = artistProfiles;
        this.artistLinks = artistLinks;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public ArtistProfileDto getMyArtistProfile() {
        var user = currentUserProvider.getCurrentUser();
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new ApiException(ErrorCode.USER_BLOCKED, "Korisnički račun je blokiran.");
        }

        var profile = artistProfiles.findById(user.getId()).orElse(null);
        List<ArtistLink> links = profile == null ? List.of()
                : artistLinks.findAllForArtistProfile(user.getId());
        return ArtistProfileMapper.toDto(user, profile, links);
    }
}
