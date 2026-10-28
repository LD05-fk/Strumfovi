package hr.fer.artconnect.opportunity.web;

import java.util.UUID;
import java.util.List;
import java.time.OffsetDateTime;
import hr.fer.artconnect.opportunity.domain.OpportunityStatus;

public record OpportunityDto(
        UUID id, String title, String description, CatalogRef type, List<CatalogRef> artFields,
        OffsetDateTime deadline, String criteria, String prize, LocationDto location,
        OffsetDateTime eventStart, OffsetDateTime eventEnd, OpportunityStatus status,
        boolean archived, boolean hiddenByAdmin, OrganizerSummary organizer,
        OffsetDateTime publishedAt, OffsetDateTime createdAt, OffsetDateTime updatedAt,
        boolean hasResults, UUID myApplicationId
) {}
