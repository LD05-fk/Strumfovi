package hr.fer.artconnect.opportunity.service.geocoding;

import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GeocodingService {
    private final NominatimClient nominatimClient;

    public GeocodingService(NominatimClient nominatimClient) {
        this.nominatimClient = nominatimClient;
    }

    public List<GeocodeResult> search(String street, String city, String postalCode) {
        return nominatimClient.search(street, city, postalCode);
    }
}
