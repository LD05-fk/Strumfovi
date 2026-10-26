package hr.fer.artconnect.artist.service;

import hr.fer.artconnect.artist.domain.ArtistLink;
import hr.fer.artconnect.artist.domain.ArtistLinkRepository;
import hr.fer.artconnect.artist.domain.ArtistProfileRepository;
import hr.fer.artconnect.artist.domain.PastExhibition;
import hr.fer.artconnect.artist.domain.PastExhibitionRepository;
import hr.fer.artconnect.artist.web.ArtistPublicProfileDto;
import hr.fer.artconnect.artist.web.PastExhibitionDto;
import hr.fer.artconnect.auth.CurrentUserProvider;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRepository;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArtistPublicProfileService {
    private final UserRepository users;
    private final ArtistProfileRepository artistProfiles;
    private final ArtistLinkRepository artistLinks;
    private final PastExhibitionRepository pastExhibitions;
    private final ArtistParticipationService participations;
    private final CurrentUserProvider currentUserProvider;

    public ArtistPublicProfileService(
            UserRepository users,
            ArtistProfileRepository artistProfiles,
            ArtistLinkRepository artistLinks,
            PastExhibitionRepository pastExhibitions,
            ArtistParticipationService participations,
            CurrentUserProvider currentUserProvider) {
        this.users = users;
        this.artistProfiles = artistProfiles;
        this.artistLinks = artistLinks;
        this.pastExhibitions = pastExhibitions;
        this.participations = participations;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public ArtistPublicProfileDto getPublicProfile(UUID artistId) {
        User artist = users.findById(artistId).orElseThrow(ArtistPublicProfileService::notFound);
        if (artist.getRole() != UserRole.ARTIST) {
            throw notFound();
        }

        User viewer = currentUserProvider.getOptionalCurrentUser().orElse(null);
        boolean isOwner = viewer != null && viewer.getId().equals(artistId);
        boolean isAdministrator = viewer != null && viewer.getRole() == UserRole.ADMIN;
        if (artist.getStatus() != UserStatus.ACTIVE
                && !(artist.getStatus() == UserStatus.BLOCKED && (isOwner || isAdministrator))) {
            throw notFound();
        }

        var profile = artistProfiles.findById(artistId).orElse(null);
        List<ArtistLink> links = profile == null ? List.of()
                : artistLinks.findAllForArtistProfile(artistId);
        List<PastExhibitionDto> exhibitions = pastExhibitions.findAllByArtistProfile_UserId(artistId).stream()
                .sorted(Comparator.comparingInt(PastExhibition::getYear).reversed()
                        .thenComparing(PastExhibition::getCreatedAt, Comparator.reverseOrder()))
                .map(exhibition -> new PastExhibitionDto(
                        exhibition.getId(), exhibition.getTitle(), exhibition.getYear(), exhibition.getPlace()))
                .toList();

        var artistProfile = ArtistProfileMapper.toDto(artist, profile, links);
        return new ArtistPublicProfileDto(
                artistProfile.userId(),
                artistProfile.displayName(),
                artistProfile.avatarUrl(),
                artistProfile.biography(),
                artistProfile.artistStatement(),
                artistProfile.artFields(),
                artistProfile.links(),
                exhibitions,
                participations.getForArtist(artistId));
    }

    private static ApiException notFound() {
        return new ApiException(ErrorCode.NOT_FOUND, "Traženi profil umjetnika nije pronađen.");
    }
}
