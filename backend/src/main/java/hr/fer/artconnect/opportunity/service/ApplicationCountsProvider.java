package hr.fer.artconnect.opportunity.service;

import hr.fer.artconnect.opportunity.web.ApplicationCounts;
import java.util.UUID;

@FunctionalInterface
public interface ApplicationCountsProvider {
    ApplicationCounts getCounts(UUID opportunityId);
}
