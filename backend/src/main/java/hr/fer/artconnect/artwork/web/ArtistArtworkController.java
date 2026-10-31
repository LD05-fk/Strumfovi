package hr.fer.artconnect.artwork.web;

import hr.fer.artconnect.artwork.service.ArtworkService;
import hr.fer.artconnect.common.ApiPaths;
import hr.fer.artconnect.common.PageRequestParams;
import hr.fer.artconnect.common.PageResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/artists")
public class ArtistArtworkController {
    private final ArtworkService artworks;

    public ArtistArtworkController(ArtworkService artworks) {
        this.artworks = artworks;
    }

    @GetMapping("/{artistId}/artworks")
    public PageResponse<ArtworkDto> listArtistArtworks(
            @PathVariable UUID artistId, @Valid @ModelAttribute PageRequestParams params) {
        return artworks.listArtistArtworks(artistId, params);
    }
}
