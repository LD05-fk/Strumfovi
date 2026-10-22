package hr.fer.artconnect.catalog.web;

import hr.fer.artconnect.catalog.domain.ArtField;
import hr.fer.artconnect.catalog.service.CatalogAdminService;
import hr.fer.artconnect.common.ApiPaths;
import hr.fer.artconnect.common.PageRequestParams;
import hr.fer.artconnect.common.PageResponse;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.V1 + "/admin/art-fields")
@PreAuthorize("hasRole('ADMIN')")
public class ArtFieldAdminController {
    private final CatalogAdminService<ArtField> service;

    public ArtFieldAdminController(CatalogAdminService<ArtField> service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<CatalogItem> list(@Valid @ModelAttribute PageRequestParams params) {
        return service.list(params);
    }

    @PostMapping
    public ResponseEntity<CatalogItem> create(@Valid @RequestBody CatalogItemInput input) {
        CatalogItem item = service.create(input);
        URI location = URI.create(ApiPaths.V1 + "/admin/art-fields/" + item.id());
        return ResponseEntity.created(location).body(item);
    }

    @PutMapping("/{itemId}")
    public CatalogItem update(@PathVariable UUID itemId, @Valid @RequestBody CatalogItemInput input) {
        return service.update(itemId, input);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> delete(@PathVariable UUID itemId) {
        service.delete(itemId);
        return ResponseEntity.noContent().build();
    }
}
