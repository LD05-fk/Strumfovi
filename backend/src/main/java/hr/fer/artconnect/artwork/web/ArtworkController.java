package hr.fer.artconnect.artwork.web;

import hr.fer.artconnect.artwork.service.ArtworkService;
import hr.fer.artconnect.common.ApiPaths;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ArtworkController.PATH)
public class ArtworkController {
    static final String PATH = ApiPaths.V1 + "/artworks";

    private final ArtworkService artworks;

    public ArtworkController(ArtworkService artworks) {
        this.artworks = artworks;
    }

    @PostMapping
    @PreAuthorize("hasRole('ARTIST')")
    public ResponseEntity<ArtworkDto> createArtwork(@Valid @RequestBody ArtworkRequest request) {
        ArtworkDto created = artworks.create(request);
        return ResponseEntity.created(URI.create(PATH + "/" + created.id())).body(created);
    }

    @GetMapping("/{artworkId}")
    public ArtworkDto getArtwork(@PathVariable UUID artworkId) {
        return artworks.getArtwork(artworkId);
    }
}
