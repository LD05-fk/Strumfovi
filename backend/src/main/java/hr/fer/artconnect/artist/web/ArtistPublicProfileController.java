package hr.fer.artconnect.artist.web;

import hr.fer.artconnect.artist.service.ArtistPublicProfileService;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ApiPaths;
import hr.fer.artconnect.common.ErrorCode;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/artists")
public class ArtistPublicProfileController {
    private final ArtistPublicProfileService artistProfiles;

    public ArtistPublicProfileController(ArtistPublicProfileService artistProfiles) {
        this.artistProfiles = artistProfiles;
    }

    @GetMapping("/{artistId}")
    public ArtistPublicProfileDto getArtistPublicProfile(@PathVariable String artistId) {
        try {
            UUID id = UUID.fromString(artistId);
            if (!id.toString().equalsIgnoreCase(artistId)) {
                throw new IllegalArgumentException("UUID nije u kanonskom obliku.");
            }
            return artistProfiles.getPublicProfile(id);
        } catch (IllegalArgumentException exception) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Traženi profil umjetnika nije pronađen.");
        }
    }
}
