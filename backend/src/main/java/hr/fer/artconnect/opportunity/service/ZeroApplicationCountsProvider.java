package hr.fer.artconnect.opportunity.service;

import hr.fer.artconnect.opportunity.web.ApplicationCounts;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ZeroApplicationCountsProvider implements ApplicationCountsProvider {
    @Override
    public ApplicationCounts getCounts(UUID opportunityId) {
        return new ApplicationCounts(0, 0, 0, 0, 0, 0);
    }
}
