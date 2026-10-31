package hr.fer.artconnect.opportunity.web;

import hr.fer.artconnect.common.ApiPaths;
import hr.fer.artconnect.opportunity.service.OpportunityService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @PutMapping("/{opportunityId}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public OpportunityDto update(@PathVariable UUID opportunityId, @Valid @RequestBody OpportunityInput input) {
        return opportunities.update(opportunityId, input);
    }

    @PostMapping("/{opportunityId}/status")
    @PreAuthorize("hasRole('ORGANIZER')")
    public OpportunityDto changeStatus(@PathVariable UUID opportunityId,
            @Valid @RequestBody OpportunityStatusChange input) {
        return opportunities.changeStatus(opportunityId, input);
    }

    @DeleteMapping("/{opportunityId}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<Void> delete(@PathVariable UUID opportunityId) {
        opportunities.delete(opportunityId);
        return ResponseEntity.noContent().build();
    }
}
