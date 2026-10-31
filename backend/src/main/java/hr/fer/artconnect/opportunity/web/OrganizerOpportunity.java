package hr.fer.artconnect.opportunity.web;

import hr.fer.artconnect.opportunity.domain.LocationType;
import hr.fer.artconnect.opportunity.domain.OpportunityStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrganizerOpportunity(
        UUID id,
        String title,
        CatalogRef type,
        List<CatalogRef> artFields,
        OffsetDateTime deadline,
        OpportunityStatus status,
        LocationType locationType,
        String city,
        BigDecimal latitude,
        BigDecimal longitude,
        OffsetDateTime eventStart,
        OrganizerSummary organizer,
        boolean archived,
        boolean hiddenByAdmin,
        ApplicationCounts applicationCounts
) {
}
