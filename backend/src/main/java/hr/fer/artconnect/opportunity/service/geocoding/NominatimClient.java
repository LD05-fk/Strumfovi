package hr.fer.artconnect.opportunity.service.geocoding;

import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class NominatimClient {
    private static final Logger log = LoggerFactory.getLogger(NominatimClient.class);
    private static final ParameterizedTypeReference<List<NominatimPlace>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final GeocodingProperties properties;
    private final NominatimRateLimiter rateLimiter;

    public NominatimClient(RestClient nominatimRestClient, GeocodingProperties properties,
            NominatimRateLimiter rateLimiter) {
        this.restClient = nominatimRestClient;
        this.properties = properties;
        this.rateLimiter = rateLimiter;
    }

    public List<GeocodeResult> search(String street, String city, String postalCode) {
        URI uri = buildSearchUri(street, city, postalCode);
        try {
            List<NominatimPlace> places = rateLimiter.execute(() -> restClient.get()
                        .uri(uri)
                        .header(HttpHeaders.USER_AGENT, properties.getUserAgent())
                        .retrieve()
                        .body(RESPONSE_TYPE));
            if (places == null) {
                throw new InvalidNominatimResponseException();
            }
            return places.stream().limit(5).map(NominatimClient::mapPlace).toList();
        } catch (RestClientException | InvalidNominatimResponseException | NumberFormatException exception) {
            log.warn("Nominatim geocoding request failed ({})", exception.getClass().getSimpleName());
            throw unavailable();
        }
    }

    private URI buildSearchUri(String street, String city, String postalCode) {
        String baseUrl = properties.getBaseUrl();
        int end = baseUrl.length();
        while (end > 0 && baseUrl.charAt(end - 1) == '/') {
            end--;
        }
        baseUrl = baseUrl.substring(0, end);
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/search")
                .queryParam("street", street)
                .queryParam("city", city);
        if (postalCode != null && !postalCode.isBlank()) {
            builder.queryParam("postalcode", postalCode);
        }
        return builder.queryParam("format", "jsonv2")
                .queryParam("addressdetails", 1)
                .queryParam("limit", 5)
                .queryParam("accept-language", "hr")
                .build()
                .encode()
                .toUri();
    }

    private static GeocodeResult mapPlace(NominatimPlace place) {
        if (place == null || place.display_name() == null || place.display_name().isBlank()
                || place.lat() == null || place.lon() == null) {
            throw new InvalidNominatimResponseException();
        }
        BigDecimal latitude = new BigDecimal(place.lat()).setScale(6, RoundingMode.HALF_UP);
        BigDecimal longitude = new BigDecimal(place.lon()).setScale(6, RoundingMode.HALF_UP);
        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0 || latitude.compareTo(BigDecimal.valueOf(90)) > 0
                || longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {
            throw new InvalidNominatimResponseException();
        }
        NominatimAddress address = place.address();
        String resultCity = address == null ? null : firstNonBlank(
                address.city(), address.town(), address.village(), address.municipality());
        String postalCode = address == null ? null : address.postcode();
        return new GeocodeResult(place.display_name(), latitude, longitude, resultCity, postalCode);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static ApiException unavailable() {
        return new ApiException(ErrorCode.GEOCODING_UNAVAILABLE,
                "Geokodiranje trenutačno nije dostupno. Pokušajte ponovno kasnije.");
    }

    record NominatimPlace(String display_name, String lat, String lon, NominatimAddress address) {}

    record NominatimAddress(
            String city, String town, String village, String municipality, String postcode) {}

    private static final class InvalidNominatimResponseException extends RuntimeException {}
}
