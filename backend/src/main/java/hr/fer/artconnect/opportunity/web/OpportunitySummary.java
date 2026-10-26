package hr.fer.artconnect.opportunity.web;

import java.util.UUID;
import java.util.List;
import java.time.OffsetDateTime;
import java.math.BigDecimal;
import hr.fer.artconnect.opportunity.domain.LocationType;
import hr.fer.artconnect.opportunity.domain.OpportunityStatus;

public record OpportunitySummary(
        UUID id, String title, CatalogRef type, List<CatalogRef> artFields,
        OffsetDateTime deadline, OpportunityStatus status, LocationType locationType,
        String city, BigDecimal latitude, BigDecimal longitude,
        OffsetDateTime eventStart, OrganizerSummary organizer
) {}
