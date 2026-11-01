package hr.fer.artconnect.opportunity.web;

import hr.fer.artconnect.common.ApiPaths;
import hr.fer.artconnect.common.PageResponse;
import hr.fer.artconnect.opportunity.service.OpportunityService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/me/opportunities")
public class OrganizerOpportunityController {
    private final OpportunityService opportunities;

    public OrganizerOpportunityController(OpportunityService opportunities) {
        this.opportunities = opportunities;
    }

    @GetMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    public PageResponse<OrganizerOpportunity> getMyOpportunities(
            @Valid @ModelAttribute OrganizerOpportunityQueryParams params) {
        return opportunities.getMyOpportunities(params);
    }
}
