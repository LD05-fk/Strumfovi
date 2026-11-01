package hr.fer.artconnect.opportunity.web;

import hr.fer.artconnect.common.PageRequestParams;
import hr.fer.artconnect.opportunity.domain.OpportunityStatus;

public class OrganizerOpportunityQueryParams extends PageRequestParams {
    private OpportunityStatus status;
    private boolean archived;

    public OpportunityStatus getStatus() {
        return status;
    }

    public void setStatus(OpportunityStatus status) {
        this.status = status;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }
}
