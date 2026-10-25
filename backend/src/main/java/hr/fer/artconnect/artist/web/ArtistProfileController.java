package hr.fer.artconnect.artist.web;

import hr.fer.artconnect.artist.service.ArtistProfileService;
import hr.fer.artconnect.common.ApiPaths;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/me/artist-profile")
@PreAuthorize("hasRole('ARTIST')")
public class ArtistProfileController {
    private final ArtistProfileService artistProfiles;

    public ArtistProfileController(ArtistProfileService artistProfiles) {
        this.artistProfiles = artistProfiles;
    }

    @GetMapping
    public ArtistProfileDto getMyArtistProfile() {
        return artistProfiles.getMyArtistProfile();
    }
}
