package hr.fer.artconnect.opportunity.service;

import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.opportunity.domain.Opportunity;
import hr.fer.artconnect.opportunity.domain.OpportunityStatus;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class OpportunityVisibilityPolicy {
    private static final Set<OpportunityStatus> PUBLIC_STATUSES = EnumSet.of(
            OpportunityStatus.OPEN, OpportunityStatus.CLOSED,
            OpportunityStatus.SELECTION, OpportunityStatus.FINISHED);

    public boolean canView(Opportunity opportunity, User viewer) {
        User organizer = opportunity.getOrganizer();
        if (viewer != null && (organizer.getId().equals(viewer.getId())
                || viewer.getRole() == UserRole.ADMIN)) {
            return true;
        }
        return PUBLIC_STATUSES.contains(opportunity.getStatus())
                && !opportunity.isArchived()
                && !opportunity.isHiddenByAdmin()
                && organizer.getStatus() != UserStatus.BLOCKED;
    }
}
