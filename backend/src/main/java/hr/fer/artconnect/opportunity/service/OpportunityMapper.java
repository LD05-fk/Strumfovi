package hr.fer.artconnect.opportunity.service;

import hr.fer.artconnect.auth.domain.OrganizerProfile;
import hr.fer.artconnect.catalog.domain.CatalogEntry;
import hr.fer.artconnect.opportunity.domain.Location;
import hr.fer.artconnect.opportunity.domain.Opportunity;
import hr.fer.artconnect.opportunity.web.ApplicationCounts;
import hr.fer.artconnect.opportunity.web.CatalogRef;
import hr.fer.artconnect.opportunity.web.LocationDto;
import hr.fer.artconnect.opportunity.web.OpportunityDto;
import hr.fer.artconnect.opportunity.web.OpportunitySummary;
import hr.fer.artconnect.opportunity.web.OrganizerOpportunity;
import hr.fer.artconnect.opportunity.web.OrganizerSummary;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OpportunityMapper {
    // Servis dohvaća profil; mapper samo oblikuje podatke, bez poziva baze.
    public OpportunityDto toDto(Opportunity opportunity, OrganizerProfile profile) {
        return new OpportunityDto(
                opportunity.getId(), opportunity.getTitle(), opportunity.getDescription(),
                catalogRef(opportunity.getType()), artFields(opportunity), opportunity.getDeadline(),
                opportunity.getCriteria(), opportunity.getPrize(), locationDto(opportunity.getLocation()),
                opportunity.getEventStart(), opportunity.getEventEnd(), opportunity.getStatus(),
                opportunity.isArchived(), opportunity.isHiddenByAdmin(), organizer(opportunity, profile),
                opportunity.getPublishedAt(), opportunity.getCreatedAt(), opportunity.getUpdatedAt(),
                false, null);
    }

    public OpportunitySummary toSummary(Opportunity opportunity, OrganizerProfile profile) {
        Location location = opportunity.getLocation();
        return new OpportunitySummary(
                opportunity.getId(), opportunity.getTitle(), catalogRef(opportunity.getType()),
                artFields(opportunity), opportunity.getDeadline(), opportunity.getStatus(),
                location.getType(), location.getCity(), location.getLatitude(), location.getLongitude(),
                opportunity.getEventStart(), organizer(opportunity, profile));
    }

    public OrganizerOpportunity toOrganizerOpportunity(Opportunity opportunity, OrganizerProfile profile,
            ApplicationCounts applicationCounts) {
        OpportunitySummary summary = toSummary(opportunity, profile);
        return new OrganizerOpportunity(
                summary.id(), summary.title(), summary.type(), summary.artFields(), summary.deadline(),
                summary.status(), summary.locationType(), summary.city(), summary.latitude(), summary.longitude(),
                summary.eventStart(), summary.organizer(), opportunity.isArchived(), opportunity.isHiddenByAdmin(),
                applicationCounts);
    }

    private CatalogRef catalogRef(CatalogEntry entry) {
        return new CatalogRef(entry.getId(), entry.getCode(), entry.getName());
    }

    private List<CatalogRef> artFields(Opportunity opportunity) {
        return opportunity.getArtFields().stream()
                .sorted(Comparator.comparingInt(CatalogEntry::getPosition).thenComparing(CatalogEntry::getCode))
                .map(this::catalogRef)
                .toList();
    }

    private OrganizerSummary organizer(Opportunity opportunity, OrganizerProfile profile) {
        var user = opportunity.getOrganizer();
        return new OrganizerSummary(user.getId(), user.getDisplayName(),
                profile == null ? null : profile.getOrganizationName());
    }

    private LocationDto locationDto(Location location) {
        return new LocationDto(location.getType(), location.getStreetAddress(), location.getCity(),
                location.getPostalCode(), location.getLatitude(), location.getLongitude(), location.getOnlineUrl());
    }
}
