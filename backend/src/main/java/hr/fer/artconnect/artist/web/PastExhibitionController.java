package hr.fer.artconnect.artist.web;

import hr.fer.artconnect.artist.service.PastExhibitionService;
import hr.fer.artconnect.common.ApiPaths;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(PastExhibitionController.PATH)
@PreAuthorize("hasRole('ARTIST')")
public class PastExhibitionController {
    static final String PATH = ApiPaths.V1 + "/past-exhibitions";

    private final PastExhibitionService pastExhibitions;

    public PastExhibitionController(PastExhibitionService pastExhibitions) {
        this.pastExhibitions = pastExhibitions;
    }

    @PostMapping
    public ResponseEntity<PastExhibitionDto> createPastExhibition(@Valid @RequestBody PastExhibitionRequest request) {
        PastExhibitionDto created = pastExhibitions.create(request);
        return ResponseEntity.created(URI.create(PATH + "/" + created.id())).body(created);
    }
}
