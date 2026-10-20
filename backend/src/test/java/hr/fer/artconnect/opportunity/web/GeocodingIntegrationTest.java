package hr.fer.artconnect.opportunity.web;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.sun.net.httpserver.HttpServer;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import hr.fer.artconnect.opportunity.service.geocoding.NominatimRateLimiter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class GeocodingIntegrationTest {
    private static final FakeNominatim NOMINATIM = new FakeNominatim();

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    MockMvc mvc;

    @MockitoBean
    NominatimRateLimiter rateLimiter;

    @DynamicPropertySource
    static void geocodingProperties(DynamicPropertyRegistry registry) {
        registry.add("artconnect.geocoding.base-url", NOMINATIM::baseUrl);
    }

    @BeforeEach
    void allowGeocodingRequest() {
        when(rateLimiter.execute(any())).thenAnswer(invocation ->
                ((Supplier<?>) invocation.getArgument(0)).get());
        NOMINATIM.respond(200, "[]");
    }

    @AfterAll
    static void stopFakeNominatim() {
        NOMINATIM.close();
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void organizatorNaCekanjuDobivaGeokodiraneRezultateIzLaznogNominatima() throws Exception {
        NOMINATIM.respond(200, """
                [{
                  "display_name":"Unska 3, Zagreb",
                  "lat":"45.8012345",
                  "lon":"15.9712345",
                  "address":{"town":"Zagreb","postcode":"10000"}
                }]
                """);

        mvc.perform(get("/api/v1/geocoding/search")
                        .param("street", "Unska 3")
                        .param("city", "Zagreb")
                        .param("postalCode", "10000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].displayName").value("Unska 3, Zagreb"))
                .andExpect(jsonPath("$.data[0].latitude").value(45.801235))
                .andExpect(jsonPath("$.data[0].longitude").value(15.971235))
                .andExpect(jsonPath("$.data[0].city").value("Zagreb"))
                .andExpect(jsonPath("$.data[0].postalCode").value("10000"));

        org.assertj.core.api.Assertions.assertThat(NOMINATIM.lastUserAgent.get())
                .isEqualTo("ArtConnect-Test/1.0");
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void nepostojecaAdresaVracaPrazanDataPopis() throws Exception {
        mvc.perform(get("/api/v1/geocoding/search").param("street", "Nepostojeca 99").param("city", "Zagreb"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void neispravniParametriVracaju400Omotnicu() throws Exception {
        mvc.perform(get("/api/v1/geocoding/search").param("street", "a").param("city", "Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void anonimniKorisnikDobiva401() throws Exception {
        mvc.perform(get("/api/v1/geocoding/search").param("street", "Unska 3").param("city", "Zagreb"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    @WithMockUser(roles = "ARTIST")
    void umjetnikDobiva403() throws Exception {
        mvc.perform(get("/api/v1/geocoding/search").param("street", "Unska 3").param("city", "Zagreb"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void limitPozivaVracenJeKao429() throws Exception {
        doThrow(new ApiException(ErrorCode.RATE_LIMITED, "Previše zahtjeva za geokodiranje."))
                .when(rateLimiter).execute(any());

        mvc.perform(get("/api/v1/geocoding/search").param("street", "Unska 3").param("city", "Zagreb"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("RATE_LIMITED"));
    }

    @Test
    @WithMockUser(roles = "ORGANIZER")
    void greskaLaznogNominatimaVracenaJeKao502() throws Exception {
        NOMINATIM.respond(500, "upstream failure");

        mvc.perform(get("/api/v1/geocoding/search").param("street", "Unska 3").param("city", "Zagreb"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("GEOCODING_UNAVAILABLE"));
    }

    private static final class FakeNominatim implements AutoCloseable {
        private final AtomicReference<FakeResponse> response = new AtomicReference<>(new FakeResponse(200, "[]"));
        private final AtomicReference<String> lastUserAgent = new AtomicReference<>();
        private final HttpServer server;

        private FakeNominatim() {
            try {
                server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
                server.createContext("/search", exchange -> {
                    lastUserAgent.set(exchange.getRequestHeaders().getFirst("User-Agent"));
                    FakeResponse current = response.get();
                    byte[] body = current.body().getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(current.status(), body.length);
                    try (var output = exchange.getResponseBody()) {
                        output.write(body);
                    }
                });
                server.start();
            } catch (IOException exception) {
                throw new IllegalStateException("Could not start fake Nominatim server", exception);
            }
        }

        private String baseUrl() {
            return "http://127.0.0.1:" + server.getAddress().getPort();
        }

        private void respond(int status, String body) {
            response.set(new FakeResponse(status, body));
        }

        @Override
        public void close() {
            server.stop(0);
        }

        private record FakeResponse(int status, String body) {}
    }
}
