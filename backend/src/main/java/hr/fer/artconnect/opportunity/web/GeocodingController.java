package hr.fer.artconnect.opportunity.web;

import hr.fer.artconnect.common.ApiPaths;
import hr.fer.artconnect.opportunity.service.geocoding.GeocodeResult;
import hr.fer.artconnect.opportunity.service.geocoding.GeocodingService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/geocoding")
public class GeocodingController {
    private final GeocodingService geocodingService;

    public GeocodingController(GeocodingService geocodingService) {
        this.geocodingService = geocodingService;
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ORGANIZER')")
    public GeocodingResponse search(
            @RequestParam @NotBlank @Size(min = 3, max = 200) String street,
            @RequestParam @NotBlank @Size(min = 2, max = 100) String city,
            @RequestParam(required = false) @Size(max = 10) String postalCode) {
        return new GeocodingResponse(geocodingService.search(street, city, postalCode));
    }

    public record GeocodingResponse(List<GeocodeResult> data) {}
}
