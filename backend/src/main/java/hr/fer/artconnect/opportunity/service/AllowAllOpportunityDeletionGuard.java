package hr.fer.artconnect.opportunity.service;

import hr.fer.artconnect.opportunity.domain.Opportunity;
import org.springframework.stereotype.Component;

@Component
class AllowAllOpportunityDeletionGuard implements OpportunityDeletionGuard {
    @Override
    public void ensureDeletable(Opportunity opportunity) {
        // OPP-04-BE2 zamjenjuje ovaj bean provjerom broja prijava i baca HAS_APPLICATIONS.
    }
}
