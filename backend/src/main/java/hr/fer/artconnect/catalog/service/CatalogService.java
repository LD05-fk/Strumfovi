package hr.fer.artconnect.catalog.service;

import hr.fer.artconnect.catalog.domain.ArtFieldRepository;
import hr.fer.artconnect.catalog.domain.OpportunityTypeRepository;
import hr.fer.artconnect.catalog.web.CatalogItem;
import hr.fer.artconnect.common.PageRequestParams;
import hr.fer.artconnect.common.PageResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogService {
    private static final Sort CATALOG_SORT = Sort.by("position", "name");

    private final ArtFieldRepository artFields;
    private final OpportunityTypeRepository opportunityTypes;

    public CatalogService(ArtFieldRepository artFields, OpportunityTypeRepository opportunityTypes) {
        this.artFields = artFields;
        this.opportunityTypes = opportunityTypes;
    }

    public PageResponse<CatalogItem> getArtFields(PageRequestParams params) {
        var page = artFields.findAllByActiveTrue(params.toPageable(CATALOG_SORT));
        return PageResponse.from(page.map(item -> new CatalogItem(
                item.getId(), item.getCode(), item.getName(), item.isActive(), item.getPosition())));
    }

    public PageResponse<CatalogItem> getOpportunityTypes(PageRequestParams params) {
        var page = opportunityTypes.findAllByActiveTrue(params.toPageable(CATALOG_SORT));
        return PageResponse.from(page.map(item -> new CatalogItem(
                item.getId(), item.getCode(), item.getName(), item.isActive(), item.getPosition())));
    }
}
