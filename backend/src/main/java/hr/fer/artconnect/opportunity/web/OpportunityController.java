package hr.fer.artconnect.opportunity.web;

import hr.fer.artconnect.common.ApiPaths;
import hr.fer.artconnect.opportunity.service.OpportunityService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(OpportunityController.PATH)
public class OpportunityController {
    static final String PATH = ApiPaths.V1 + "/opportunities";
    private final OpportunityService opportunities;

    public OpportunityController(OpportunityService opportunities) {
        this.opportunities = opportunities;
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<OpportunityDto> create(@Valid @RequestBody OpportunityInput input) {
        OpportunityDto created = opportunities.create(input);
        return ResponseEntity.created(URI.create(PATH + "/" + created.id())).body(created);
    }
}
