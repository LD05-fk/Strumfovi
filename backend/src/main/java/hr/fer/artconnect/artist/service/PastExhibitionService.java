package hr.fer.artconnect.artist.service;

import hr.fer.artconnect.artist.domain.ArtistProfile;
import hr.fer.artconnect.artist.domain.ArtistProfileRepository;
import hr.fer.artconnect.artist.domain.PastExhibition;
import hr.fer.artconnect.artist.domain.PastExhibitionRepository;
import hr.fer.artconnect.artist.web.PastExhibitionDto;
import hr.fer.artconnect.artist.web.PastExhibitionRequest;
import hr.fer.artconnect.auth.CurrentUserProvider;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class PastExhibitionService {
    private final PastExhibitionRepository pastExhibitions;
    private final ArtistProfileRepository artistProfiles;
    private final CurrentUserProvider currentUserProvider;

    public PastExhibitionService(PastExhibitionRepository pastExhibitions,
            ArtistProfileRepository artistProfiles, CurrentUserProvider currentUserProvider) {
        this.pastExhibitions = pastExhibitions;
        this.artistProfiles = artistProfiles;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public PastExhibitionDto create(PastExhibitionRequest request) {
        User user = currentUserProvider.getCurrentUser();
        // Umjetnik koji još nije spremio profil nema zapis u artist_profiles,
        // a strani ključ izložbe upućuje na njega, pa se prazan profil stvara pri prvom unosu.
        ArtistProfile profile = artistProfiles.findById(user.getId())
            .orElseGet(() -> artistProfiles.save(ArtistProfile.forUser(user)));
        PastExhibition saved = pastExhibitions.save(PastExhibition.create(
            profile, request.title(), request.year().shortValue(), request.place()));
        return toDto(saved);
    }

    @Transactional
    public PastExhibitionDto update(UUID pastExhibitionId, PastExhibitionRequest request) {
        PastExhibition exhibition = pastExhibitions.findById(pastExhibitionId)
                .orElseThrow(PastExhibitionService::exhibitionNotFound);
        User user = currentUserProvider.getCurrentUser();
        if (!exhibition.getArtistProfile().getUserId().equals(user.getId())) {
            throw exhibitionNotFound();
        }
        exhibition.update(request.title(), request.year().shortValue(), request.place());
        return toDto(exhibition);
    }

    private static ApiException exhibitionNotFound() {
        return new ApiException(ErrorCode.NOT_FOUND, "Izložba umjetnika nije pronađena.");
    }

    private static PastExhibitionDto toDto(PastExhibition exhibition) {
        return new PastExhibitionDto(
            exhibition.getId(), exhibition.getTitle(), exhibition.getYear(), exhibition.getPlace()
        );
    }
}
