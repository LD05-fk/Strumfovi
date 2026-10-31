package hr.fer.artconnect.opportunity.service;

import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.opportunity.domain.Opportunity;

/** Provjera smije li se prilika obrisati. */
public interface OpportunityDeletionGuard {
    /** @throws ApiException HAS_APPLICATIONS kad prilika ima prijave. */
    void ensureDeletable(Opportunity opportunity);
}
