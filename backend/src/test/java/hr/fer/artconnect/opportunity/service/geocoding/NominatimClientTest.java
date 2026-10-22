package hr.fer.artconnect.opportunity.service.geocoding;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

class NominatimClientTest {
    private static final String USER_AGENT = "ArtConnect-Test/1.0";
    private MockRestServiceServer server;
    private NominatimClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        GeocodingProperties properties = properties("http://nominatim.test", USER_AGENT,
                Duration.ZERO, Duration.ofSeconds(2));
        client = new NominatimClient(builder.build(), properties,
                new NominatimRateLimiter(properties, Clock.systemUTC()));
    }

    @Test
    void mapiraKoordinateGradIzTownPostanskiBrojIUserAgent() {
        server.expect(requestTo(containsString("/search")))
                .andExpect(queryParam("street", "Unska%203"))
                .andExpect(queryParam("city", "Zagreb"))
                .andExpect(queryParam("postalcode", "10000"))
                .andExpect(queryParam("format", "jsonv2"))
                .andExpect(queryParam("addressdetails", "1"))
                .andExpect(queryParam("limit", "5"))
                .andExpect(queryParam("accept-language", "hr"))
                .andExpect(header(HttpHeaders.USER_AGENT, USER_AGENT))
                .andRespond(withSuccess("""
                        [{
                          "display_name":"Unska 3, Zagreb",
                          "lat":"45.80123456",
                          "lon":"15.97123456",
                          "address":{"town":"Zagreb","postcode":"10000"}
                        }]
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.search("Unska 3", "Zagreb", "10000"))
                .containsExactly(new GeocodeResult("Unska 3, Zagreb",
                        new java.math.BigDecimal("45.801235"), new java.math.BigDecimal("15.971235"),
                        "Zagreb", "10000"));
        server.verify();
    }

    @Test
    void mapiraVillageIGradPremaPrioritetnomRedoslijedu() {
        server.expect(requestTo(containsString("/search")))
                .andExpect(header(HttpHeaders.USER_AGENT, USER_AGENT))
                .andRespond(withSuccess("""
                        [
                          {"display_name":"Mjesto A","lat":"45.1","lon":"15.2",
                           "address":{"village":"Selo","municipality":"Općina","postcode":"12345"}},
                          {"display_name":"Mjesto B","lat":"46.1","lon":"16.2",
                           "address":{"municipality":"Općina"}}
                        ]
                        """, MediaType.APPLICATION_JSON));

        var results = client.search("Ulica 1", "Grad", null);

        assertThat(results).extracting(GeocodeResult::city).containsExactly("Selo", "Općina");
        assertThat(results.get(0).postalCode()).isEqualTo("12345");
        server.verify();
    }

    @Test
    void nemaPostalcodeParametraKadNijeProslijeden() {
        server.expect(requestTo(allOf(containsString("street=Ulica"), not(containsString("postalcode=")))))
                .andExpect(header(HttpHeaders.USER_AGENT, USER_AGENT))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.search("Ulica", "Grad", null)).isEmpty();
        server.verify();
    }

    @Test
    void ogranicavaOdgovorNaPetRezultata() {
        server.expect(requestTo(containsString("/search")))
                .andExpect(header(HttpHeaders.USER_AGENT, USER_AGENT))
                .andRespond(withSuccess("""
                        [
                          {"display_name":"1","lat":"45","lon":"15"},
                          {"display_name":"2","lat":"45","lon":"15"},
                          {"display_name":"3","lat":"45","lon":"15"},
                          {"display_name":"4","lat":"45","lon":"15"},
                          {"display_name":"5","lat":"45","lon":"15"},
                          {"display_name":"6","lat":"45","lon":"15"}
                        ]
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.search("Ulica", "Grad", null)).hasSize(5);
        server.verify();
    }

    @Test
    void http500PostajeGeocodingUnavailable() {
        server.expect(requestTo(containsString("/search")))
                .andExpect(header(HttpHeaders.USER_AGENT, USER_AGENT))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.search("Ulica", "Grad", null))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.GEOCODING_UNAVAILABLE);
        server.verify();
    }

    @Test
    void timeoutPostajeGeocodingUnavailable() {
        server.expect(requestTo(containsString("/search")))
                .andExpect(header(HttpHeaders.USER_AGENT, USER_AGENT))
                .andRespond(request -> {
                    throw new ResourceAccessException("timeout");
                });

        assertThatThrownBy(() -> client.search("Ulica", "Grad", null))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.GEOCODING_UNAVAILABLE);
        server.verify();
    }

    @Test
    void neispravanJsonPostajeGeocodingUnavailable() {
        server.expect(requestTo(containsString("/search")))
                .andExpect(header(HttpHeaders.USER_AGENT, USER_AGENT))
                .andRespond(withSuccess("{", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.search("Ulica", "Grad", null))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.GEOCODING_UNAVAILABLE);
        server.verify();
    }

    @Test
    void neispravneKoordinatePostajuGeocodingUnavailable() {
        server.expect(requestTo(containsString("/search")))
                .andExpect(header(HttpHeaders.USER_AGENT, USER_AGENT))
                .andRespond(withSuccess("""
                        [{"display_name":"Ulica","lat":"not-a-number","lon":"15"}]
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.search("Ulica", "Grad", null))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.GEOCODING_UNAVAILABLE);
        server.verify();
    }

    @Test
    void koordinateIzvanGeografskihGranicaPostajuGeocodingUnavailable() {
        server.expect(requestTo(containsString("/search")))
                .andExpect(header(HttpHeaders.USER_AGENT, USER_AGENT))
                .andRespond(withSuccess("""
                        [{"display_name":"Ulica","lat":"91","lon":"15"}]
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.search("Ulica", "Grad", null))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.GEOCODING_UNAVAILABLE);
        server.verify();
    }

    private static GeocodingProperties properties(String baseUrl, String userAgent,
            Duration minInterval, Duration maxWait) {
        GeocodingProperties properties = new GeocodingProperties();
        properties.setBaseUrl(baseUrl);
        properties.setUserAgent(userAgent);
        properties.setMinInterval(minInterval);
        properties.setMaxWait(maxWait);
        return properties;
    }
}
