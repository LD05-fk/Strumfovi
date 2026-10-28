package hr.fer.artconnect.opportunity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.support.IntegrationTestBase;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

class OpportunityIntegrationTest extends IntegrationTestBase {
    private static final String PATH = "/api/v1/opportunities";
    private static final String NOW = "2026-10-26T10:00:00Z";
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean(name = "geocodingClock") Clock clock;
    private UUID typeId;
    private UUID fieldId;

    @BeforeEach
    void catalogsAndClock() {
        when(clock.instant()).thenReturn(Instant.parse(NOW));
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        typeId = jdbc.queryForObject("SELECT id FROM opportunity_types WHERE active = true LIMIT 1", UUID.class);
        fieldId = jdbc.queryForObject("SELECT id FROM art_fields WHERE active = true LIMIT 1", UUID.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"APPROVED", "PENDING"})
    void organizatorSpremaFizickuPrilikuKaoPrivatniNacrt(String approval) throws Exception {
        UUID owner = loginAs("ORGANIZER");
        jdbc.update("UPDATE users SET organizer_approval = ? WHERE id = ?", approval, owner);
        jdbc.update("INSERT INTO organizer_profiles (user_id, organization_name) VALUES (?, 'Galerija')", owner);
        var request = validRequest();
        request.put("organizerId", UUID.randomUUID());
        request.put("status", "OPEN");
        var response = send(request)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Izložba"))
                .andExpect(jsonPath("$.description").value("Opis izložbe"))
                .andExpect(jsonPath("$.status").value("PREPARATION"))
                .andExpect(jsonPath("$.archived").value(false))
                .andExpect(jsonPath("$.hiddenByAdmin").value(false))
                .andExpect(jsonPath("$.publishedAt").value(nullValue()))
                .andExpect(jsonPath("$.criteria").value(nullValue()))
                .andExpect(jsonPath("$.prize").value(nullValue()))
                .andExpect(jsonPath("$.hasResults").value(false))
                .andExpect(jsonPath("$.myApplicationId").value(nullValue()))
                .andExpect(jsonPath("$.organizer.id").value(owner.toString()))
                .andExpect(jsonPath("$.organizer.organizationName").value("Galerija"))
                .andExpect(jsonPath("$.type.id").value(typeId.toString()))
                .andExpect(jsonPath("$.artFields[0].id").value(fieldId.toString()))
                .andExpect(jsonPath("$.location.latitude").value(45.123457))
                .andExpect(jsonPath("$.location.longitude").value(16.123457))
                .andExpect(jsonPath("$.location.onlineUrl").value(nullValue()))
                .andReturn().getResponse();
        String id = objectMapper.readTree(response.getContentAsString()).get("id").asString();
        assertThat(response.getHeader("Location")).isEqualTo(PATH + "/" + id);
        var row = jdbc.queryForMap("SELECT * FROM opportunities WHERE id = ?", UUID.fromString(id));
        assertThat(row.get("organizer_id")).isEqualTo(owner);
        assertThat(row.get("status")).isEqualTo("PREPARATION");
        assertThat(row.get("published_at")).isNull();
        assertThat(row.get("online_url")).isNull();
        assertThat((BigDecimal) row.get("latitude")).isEqualByComparingTo("45.123457");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM opportunity_art_fields WHERE opportunity_id = ?",
                Integer.class, UUID.fromString(id))).isEqualTo(1);
    }

    @Test
    void onlineLokacijaOdbacujeFizickaPoljaISpremaDatumeSPomakom() throws Exception {
        loginAs("ORGANIZER");
        var request = validRequest();
        var location = location(request);
        location.put("type", "ONLINE");
        location.put("onlineUrl", " https://example.org/event ");
        location.put("latitude", 999);
        request.put("eventStart", "2026-11-01T14:00:00+02:00");
        request.put("eventEnd", "2026-11-01T13:00:00Z");
        send(request).andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.location.type").value("ONLINE"))
                .andExpect(jsonPath("$.location.onlineUrl").value("https://example.org/event"))
                .andExpect(jsonPath("$.location.streetAddress").value(nullValue()))
                .andExpect(jsonPath("$.location.city").value(nullValue()))
                .andExpect(jsonPath("$.location.postalCode").value(nullValue()))
                .andExpect(jsonPath("$.location.latitude").value(nullValue()))
                .andExpect(jsonPath("$.location.longitude").value(nullValue()));
        var row = jdbc.queryForMap("SELECT * FROM opportunities");
        assertThat(row.get("street_address")).isNull();
        assertThat(row.get("city")).isNull();
        assertThat(row.get("postal_code")).isNull();
        assertThat(row.get("latitude")).isNull();
        assertThat(row.get("longitude")).isNull();
        assertThat(row.get("online_url")).isEqualTo("https://example.org/event");
    }

    @ParameterizedTest
    @ValueSource(strings = {"title", "description", "typeId", "artFieldIds", "deadline", "location"})
    void nedostajuceObaveznoPoljeVraca400SDetaljem(String field) throws Exception {
        loginAs("ORGANIZER");
        var request = validRequest();
        request.remove(field);
        invalid(request, field);
    }

    @ParameterizedTest
    @ValueSource(strings = {"title", "description", "criteria", "prize", "location.streetAddress",
            "location.city", "location.postalCode", "location.latitude", "location.longitude"})
    void ogranicenjaDtoaVraca400SPravimPoljem(String field) throws Exception {
        loginAs("ORGANIZER");
        var request = validRequest();
        switch (field) {
            case "title" -> request.put(field, " ab ");
            case "description" -> request.put(field, "x".repeat(10001));
            case "criteria" -> request.put(field, "x".repeat(5001));
            case "prize" -> request.put(field, "x".repeat(2001));
            case "location.streetAddress" -> location(request).put("streetAddress", "x".repeat(201));
            case "location.city" -> location(request).put("city", "x".repeat(101));
            case "location.postalCode" -> location(request).put("postalCode", "x".repeat(11));
            case "location.latitude" -> location(request).put("latitude", new BigDecimal("90.0000001"));
            case "location.longitude" -> location(request).put("longitude", new BigDecimal("-180.0000001"));
            default -> throw new IllegalArgumentException(field);
        }
        invalid(request, field);
    }

    @ParameterizedTest
    @ValueSource(strings = {"deadline", "eventEnd", "artFieldIds", "location.streetAddress", "location.city",
            "location.latitude", "location.longitude", "location.onlineUrl"})
    void poslovnaPravilaVraca400BezSpremanja(String field) throws Exception {
        loginAs("ORGANIZER");
        var request = validRequest();
        switch (field) {
            case "deadline" -> request.put(field, NOW);
            case "eventEnd" -> request.put(field, "2026-11-01T10:00:00Z");
            case "artFieldIds" -> request.put(field, List.of(fieldId, fieldId));
            case "location.onlineUrl" -> {
                location(request).put("type", "ONLINE");
                location(request).put("onlineUrl", "ftp://example.org");
            }
            default -> location(request).remove(field.substring("location.".length()));
        }
        invalid(request, field);
    }

    @ParameterizedTest
    @ValueSource(strings = {"typeId", "artFieldIds"})
    void neaktivniINepostojeciSifrarniciVraca400(String field) throws Exception {
        loginAs("ORGANIZER");
        var request = validRequest();
        String table = field.equals("typeId") ? "opportunity_types" : "art_fields";
        UUID id = field.equals("typeId") ? typeId : fieldId;
        // HTTP test ne držimo u testnoj transakciji: spremanje sesije ima vlastitu transakciju.
        jdbc.update("UPDATE " + table + " SET active = false WHERE id = ?", id);
        try {
            invalid(request, field);
            request.put(field, field.equals("typeId") ? UUID.randomUUID() : List.of(UUID.randomUUID()));
            invalid(request, field);
        } finally {
            jdbc.update("UPDATE " + table + " SET active = true WHERE id = ?", id);
        }
    }

    @Test
    void neispravniJsonTipoviVraca400() throws Exception {
        loginAs("ORGANIZER");
        var request = validRequest();
        request.put("typeId", "nije-uuid");
        send(request).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        request = validRequest();
        location(request).put("type", "UNKNOWN");
        send(request).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        assertEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ARTIST", "ADMIN"})
    void ostaleUlogeVraca403(String role) throws Exception {
        loginAs(role);
        send(validRequest()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        assertEmpty();
    }

    @Test
    void bezSesijeVraca401() throws Exception {
        mockMvc.perform(post(PATH).with(anonymous()).with(withCsrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        assertEmpty();
    }

    @Test
    void bezCsrfTokenaVraca403() throws Exception {
        loginAs("ORGANIZER");
        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        assertEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ONBOARDING", "BLOCKED", "DELETED"})
    void provjeravaAktualniStatusRacunaIzBaze(String userStatus) throws Exception {
        UUID owner = loginAs("ORGANIZER");
        jdbc.update("UPDATE users SET status = ? WHERE id = ?", userStatus, owner);
        String code = switch (userStatus) {
            case "ONBOARDING" -> "ONBOARDING_REQUIRED";
            case "BLOCKED" -> "USER_BLOCKED";
            default -> "FORBIDDEN";
        };
        send(validRequest()).andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value(code));
        assertEmpty();
    }

    @Test
    void zastarjelaUlogaUSesijiNeDopustaStvaranje() throws Exception {
        UUID owner = loginAs("ORGANIZER");
        jdbc.update("UPDATE users SET role = 'ARTIST', organizer_approval = NULL WHERE id = ?", owner);
        send(validRequest()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        assertEmpty();
    }

    private void invalid(Map<String, Object> request, String field) throws Exception {
        send(request).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[*].field", hasItem(field)));
        assertEmpty();
    }

    private void assertEmpty() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM opportunities", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM opportunity_art_fields", Integer.class)).isZero();
    }

    private ResultActions send(Map<String, Object> request) throws Exception {
        return mockMvc.perform(post(PATH).with(withCsrf()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> location(Map<String, Object> request) {
        return (Map<String, Object>) request.get("location");
    }

    private Map<String, Object> validRequest() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("title", " Izložba ");
        request.put("description", " Opis izložbe ");
        request.put("typeId", typeId);
        request.put("artFieldIds", List.of(fieldId));
        request.put("deadline", "2026-11-01T10:00:00+01:00");
        request.put("criteria", " ");
        request.put("prize", " ");
        request.put("location", new LinkedHashMap<>(Map.of(
                "type", "PHYSICAL", "streetAddress", " Ilica 1 ", "city", " Zagreb ", "postalCode", "10000",
                "latitude", new BigDecimal("45.1234567"), "longitude", new BigDecimal("16.1234567"),
                "onlineUrl", "https://ignored.org")));
        return request;
    }
}
