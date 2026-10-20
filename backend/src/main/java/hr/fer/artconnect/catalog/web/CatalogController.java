package hr.fer.artconnect.catalog.web;

import hr.fer.artconnect.catalog.service.CatalogService;
import hr.fer.artconnect.common.PageRequestParams;
import hr.fer.artconnect.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CatalogController {
    private final CatalogService catalogs;

    public CatalogController(CatalogService catalogs) {
        this.catalogs = catalogs;
    }

    @GetMapping("/art-fields")
    public PageResponse<CatalogItem> getArtFields(@Valid @ModelAttribute PageRequestParams params) {
        return catalogs.getArtFields(params);
    }

    @GetMapping("/opportunity-types")
    public PageResponse<CatalogItem> getOpportunityTypes(@Valid @ModelAttribute PageRequestParams params) {
        return catalogs.getOpportunityTypes(params);
    }
}
