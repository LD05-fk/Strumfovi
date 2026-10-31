package hr.fer.artconnect.opportunity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.auth.domain.UserRepository;
import hr.fer.artconnect.support.IntegrationTestBase;
import hr.fer.artconnect.support.TestPrincipals;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

class OpportunityIntegrationTest extends IntegrationTestBase {
    private static final String PATH = "/api/v1/opportunities";
    private static final String NOW = "2026-10-26T10:00:00Z";
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository users;
    @Autowired EntityManagerFactory entityManagerFactory;
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

    @ParameterizedTest
    @ValueSource(strings = {"PREPARATION", "OPEN", "CLOSED", "SELECTION", "FINISHED"})
    void vlasnikUredujePrilikuBezPromjeneStatusa(String opportunityStatus) throws Exception {
        UUID owner = loginAs("ORGANIZER");
        var extraFields = jdbc.queryForList(
                "SELECT id FROM art_fields WHERE active = true AND id <> ? LIMIT 2", UUID.class, fieldId);
        var request = validRequest();
        request.put("artFieldIds", List.of(fieldId, extraFields.get(0)));
        request.put("criteria", "Stari kriteriji");
        UUID id = createOpportunity(request);
        jdbc.update("UPDATE opportunities SET status = ?, archived = true, hidden_by_admin = true, "
                + "created_at = '2026-10-25T10:00:00Z', updated_at = '2026-10-25T10:00:00Z' WHERE id = ?",
                opportunityStatus, id);
        var preserved = jdbc.queryForMap("SELECT organizer_id, status, archived, hidden_by_admin, "
                + "created_at, published_at, hidden_at FROM opportunities WHERE id = ?", id);
        jdbc.update("UPDATE opportunity_types SET active = false WHERE id = ?", typeId);
        jdbc.update("UPDATE art_fields SET active = false WHERE id = ?", fieldId);
        try {
            request.put("title", "Uređena prilika");
            request.remove("criteria");
            request.put("deadline", "2026-10-01T10:00:00Z");
            request.put("artFieldIds", List.of(fieldId, extraFields.get(1)));
            request.put("location", Map.of("type", "ONLINE", "onlineUrl", "https://example.org/event"));
            update(id, request).andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Uređena prilika"))
                    .andExpect(jsonPath("$.status").value(opportunityStatus))
                    .andExpect(jsonPath("$.organizer.id").value(owner.toString()))
                    .andExpect(jsonPath("$.updatedAt").value(NOW));
            assertThat(jdbc.queryForMap("SELECT * FROM opportunities WHERE id = ?", id))
                    .containsAllEntriesOf(preserved)
                    .containsEntry("title", "Uređena prilika")
                    .containsEntry("criteria", null)
                    .containsEntry("street_address", null).containsEntry("city", null)
                    .containsEntry("postal_code", null).containsEntry("latitude", null)
                    .containsEntry("longitude", null).containsEntry("online_url", "https://example.org/event");
            assertThat(jdbc.queryForList("SELECT art_field_id FROM opportunity_art_fields WHERE opportunity_id = ?",
                    UUID.class, id)).containsExactlyInAnyOrder(fieldId, extraFields.get(1));
            assertThat(jdbc.queryForObject("SELECT deadline < updated_at FROM opportunities WHERE id = ?",
                    Boolean.class, id)).isTrue();
        } finally {
            jdbc.update("UPDATE opportunity_types SET active = true WHERE id = ?", typeId);
            jdbc.update("UPDATE art_fields SET active = true WHERE id = ?", fieldId);
        }
    }

    @Test
    void organizatorDohvacaSamoSvojePrilikeUArhivskimIStatusnimFiltrima() throws Exception {
        UUID owner = loginAs("ORGANIZER");
        UUID preparation = createOpportunity(validRequest());
        UUID open = createOpportunity(validRequest());
        UUID closed = createOpportunity(validRequest());
        UUID archived = createOpportunity(validRequest());
        jdbc.update("UPDATE opportunities SET created_at = '2026-10-24T10:00:00Z' WHERE id = ?", preparation);
        jdbc.update("UPDATE opportunities SET status = 'OPEN', hidden_by_admin = true, "
                + "created_at = '2026-10-26T09:00:00Z' WHERE id = ?", open);
        jdbc.update("UPDATE opportunities SET status = 'CLOSED', "
                + "created_at = '2026-10-25T10:00:00Z' WHERE id = ?", closed);
        jdbc.update("UPDATE opportunities SET status = 'OPEN', archived = true, hidden_by_admin = true, "
                + "created_at = '2026-10-26T08:00:00Z' WHERE id = ?", archived);

        UUID otherOwner = loginAs("ORGANIZER");
        UUID otherOpportunity = createOpportunity(validRequest());
        TestSecurityContextHolder.setAuthentication(
                TestPrincipals.authentication(users.findById(owner).orElseThrow()));

        mockMvc.perform(get("/api/v1/me/opportunities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].id").value(open.toString()))
                .andExpect(jsonPath("$.data[0].hiddenByAdmin").value(true))
                .andExpect(jsonPath("$.data[1].id").value(closed.toString()))
                .andExpect(jsonPath("$.data[2].id").value(preparation.toString()))
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.pageSize").value(20))
                .andExpect(jsonPath("$.meta.total").value(3))
                .andExpect(jsonPath("$.data[0].applicationCounts.total").value(0))
                .andExpect(jsonPath("$.data[0].applicationCounts.submitted").value(0))
                .andExpect(jsonPath("$.data[0].applicationCounts.underReview").value(0))
                .andExpect(jsonPath("$.data[0].applicationCounts.needsInfo").value(0))
                .andExpect(jsonPath("$.data[0].applicationCounts.accepted").value(0))
                .andExpect(jsonPath("$.data[0].applicationCounts.rejected").value(0));

        mockMvc.perform(get("/api/v1/me/opportunities").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(open.toString()));

        mockMvc.perform(get("/api/v1/me/opportunities").param("archived", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(archived.toString()))
                .andExpect(jsonPath("$.data[0].archived").value(true))
                .andExpect(jsonPath("$.data[0].hiddenByAdmin").value(true));

        assertThat(otherOwner).isNotEqualTo(owner);
        assertThat(otherOpportunity).isNotIn(open, closed, preparation, archived);
    }

    @Test
    void artistNeMozeDohvatitiOrganizatorovePrilike() throws Exception {
        loginAs("ARTIST");

        mockMvc.perform(get("/api/v1/me/opportunities"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void anonimniKorisnikNeMozeDohvatitiOrganizatorovePrilike() throws Exception {
        mockMvc.perform(get("/api/v1/me/opportunities").with(anonymous()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void neispravanFilterVracaValidationError() throws Exception {
        loginAs("ORGANIZER");

        mockMvc.perform(get("/api/v1/me/opportunities").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void dohvatStraniceNeRadiNPlusJedanZaVrstuIPodrucja() throws Exception {
        loginAs("ORGANIZER");
        UUID first = createOpportunity(validRequest());
        UUID second = createOpportunity(validRequest());
        UUID third = createOpportunity(validRequest());
        jdbc.update("UPDATE opportunities SET archived = true WHERE id IN (?, ?)", second, third);

        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();
        try {
            mockMvc.perform(get("/api/v1/me/opportunities"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1));
            long oneOpportunityStatements = statistics.getPrepareStatementCount();

            jdbc.update("UPDATE opportunities SET archived = false WHERE id IN (?, ?, ?)", first, second, third);
            statistics.clear();
            mockMvc.perform(get("/api/v1/me/opportunities"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(3));

            assertThat(statistics.getPrepareStatementCount()).isEqualTo(oneOpportunityStatements);
        } finally {
            statistics.setStatisticsEnabled(false);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"typeId", "artFieldIds"})
    void uredivanjeOdbijaNoviNeaktivniSifrarnikBezPromjena(String field) throws Exception {
        loginAs("ORGANIZER");
        UUID id = createOpportunity(validRequest());
        String table = field.equals("typeId") ? "opportunity_types" : "art_fields";
        UUID current = field.equals("typeId") ? typeId : fieldId;
        UUID inactive = jdbc.queryForObject("SELECT id FROM " + table + " WHERE active = true AND id <> ? LIMIT 1",
                UUID.class, current);
        var before = jdbc.queryForMap("SELECT * FROM opportunities WHERE id = ?", id);
        jdbc.update("UPDATE " + table + " SET active = false WHERE id = ?", inactive);
        try {
            var request = validRequest();
            request.put("title", "Ne smije se spremiti");
            request.put(field, field.equals("typeId") ? inactive : List.of(inactive));
            update(id, request).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.error.details[*].field", hasItem(field)));
            assertThat(jdbc.queryForMap("SELECT * FROM opportunities WHERE id = ?", id)).isEqualTo(before);
            assertThat(jdbc.queryForList("SELECT art_field_id FROM opportunity_art_fields WHERE opportunity_id = ?",
                    UUID.class, id)).containsExactly(fieldId);
        } finally {
            jdbc.update("UPDATE " + table + " SET active = true WHERE id = ?", inactive);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ORGANIZER", "ARTIST"})
    void drugiKorisnikNeMozeUreditiPriliku(String role) throws Exception {
        loginAs("ORGANIZER");
        UUID id = createOpportunity(validRequest());
        var before = jdbc.queryForMap("SELECT * FROM opportunities WHERE id = ?", id);
        loginAs(role);
        update(id, validRequest()).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        assertThat(jdbc.queryForMap("SELECT * FROM opportunities WHERE id = ?", id)).isEqualTo(before);
    }

    @Test
    void uredivanjeTraziSesijuIPostojecuPriliku() throws Exception {
        mockMvc.perform(put(PATH + "/" + UUID.randomUUID()).with(anonymous()).with(withCsrf())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        loginAs("ORGANIZER");
        update(UUID.randomUUID(), validRequest()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    private UUID createOpportunity(Map<String, Object> request) throws Exception {
        var response = send(request).andExpect(status().isCreated()).andReturn().getResponse();
        return UUID.fromString(objectMapper.readTree(response.getContentAsString()).get("id").asString());
    }

    private ResultActions update(UUID id, Map<String, Object> request) throws Exception {
        return mockMvc.perform(put(PATH + "/" + id).with(withCsrf()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
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
