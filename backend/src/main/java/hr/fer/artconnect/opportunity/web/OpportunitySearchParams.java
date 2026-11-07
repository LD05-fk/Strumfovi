package hr.fer.artconnect.opportunity.web;

import hr.fer.artconnect.common.PageRequestParams;
import hr.fer.artconnect.opportunity.domain.LocationType;
import hr.fer.artconnect.opportunity.domain.OpportunityStatus;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class OpportunitySearchParams extends PageRequestParams {
    @Size(max = 100, message = "{validation.opportunity.search.q.max}")
    private String q;
    private List<UUID> artFieldIds;
    private List<UUID> typeIds;
    @Size(max = 100, message = "{validation.opportunity.search.city.max}")
    private String city;
    private LocationType locationType;
    private Set<OpportunityStatus> statuses;
    private LocalDate deadlineFrom;
    private LocalDate deadlineTo;
    private String sort;

    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }

    public List<UUID> getArtFieldIds() { return artFieldIds; }
    public void setArtFieldIds(List<UUID> artFieldIds) { this.artFieldIds = artFieldIds; }

    public List<UUID> getTypeIds() { return typeIds; }
    public void setTypeIds(List<UUID> typeIds) { this.typeIds = typeIds; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public LocationType getLocationType() { return locationType; }
    public void setLocationType(LocationType locationType) { this.locationType = locationType; }

    public Set<OpportunityStatus> getStatuses() { return statuses; }
    public void setStatuses(Set<OpportunityStatus> statuses) { this.statuses = statuses; }

    public LocalDate getDeadlineFrom() { return deadlineFrom; }
    public void setDeadlineFrom(LocalDate deadlineFrom) { this.deadlineFrom = deadlineFrom; }

    public LocalDate getDeadlineTo() { return deadlineTo; }
    public void setDeadlineTo(LocalDate deadlineTo) { this.deadlineTo = deadlineTo; }

    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
}
