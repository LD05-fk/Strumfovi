package hr.fer.artconnect.opportunity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.auth.domain.UserRepository;
import hr.fer.artconnect.support.IntegrationTestBase;
import hr.fer.artconnect.support.TestPrincipals;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

class OpportunityApiIT extends IntegrationTestBase {
    private static final String PATH = "/api/v1/opportunities";
    private static final Instant NOW = Instant.parse("2026-10-26T10:00:00Z");

    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper objectMapper;
    @Autowired UserRepository users;
    @MockitoBean(name = "geocodingClock") Clock clock;

    private UUID ownerId;
    private UUID opportunityId;
    private UUID typeId;
    private UUID artFieldId;

    @BeforeEach
    void createOpportunityOwnedByOrganizer() throws Exception {
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        ownerId = loginAs("ORGANIZER");
        typeId = jdbc.queryForObject(
                "SELECT id FROM opportunity_types WHERE active = true LIMIT 1", UUID.class);
        artFieldId = jdbc.queryForObject(
                "SELECT id FROM art_fields WHERE active = true LIMIT 1", UUID.class);

        opportunityId = createOpportunity("Izložba za uređivanje", "Početni opis");
    }

    @Test
    void drugiOrganizatorNeMozeUreditiTuduPriliku() throws Exception {
        loginAs("ORGANIZER");

        mockMvc.perform(put(PATH + "/" + opportunityId)
                        .with(withCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(opportunityInput("Neovlaštena izmjena", "Izmijenjen opis")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        assertThat(jdbc.queryForObject(
                "SELECT title FROM opportunities WHERE id = ?", String.class, opportunityId))
                .isEqualTo("Izložba za uređivanje");
        assertThat(jdbc.queryForObject(
                "SELECT description FROM opportunities WHERE id = ?", String.class, opportunityId))
                .isEqualTo("Početni opis");
        assertThat(jdbc.queryForObject(
                "SELECT organizer_id FROM opportunities WHERE id = ?", UUID.class, opportunityId))
                .isEqualTo(ownerId);
    }

    @Test
    void vlasnikMozeUreditiSvojuPriliku() throws Exception {
        mockMvc.perform(put(PATH + "/" + opportunityId)
                        .with(withCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(opportunityInput("Uređena izložba", "Ažurirani opis")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(opportunityId.toString()))
                .andExpect(jsonPath("$.title").value("Uređena izložba"))
                .andExpect(jsonPath("$.description").value("Ažurirani opis"))
                .andExpect(jsonPath("$.organizer.id").value(ownerId.toString()))
                .andExpect(jsonPath("$.status").value("PREPARATION"));

        assertThat(jdbc.queryForObject(
                "SELECT title FROM opportunities WHERE id = ?", String.class, opportunityId))
                .isEqualTo("Uređena izložba");
        assertThat(jdbc.queryForObject(
                "SELECT description FROM opportunities WHERE id = ?", String.class, opportunityId))
                .isEqualTo("Ažurirani opis");
        assertThat(jdbc.queryForObject(
                "SELECT organizer_id FROM opportunities WHERE id = ?", UUID.class, opportunityId))
                .isEqualTo(ownerId);
    }

    // OPP-03-TEST: puni životni ciklus prilike preko krajnje točke statusa i javne pretrage.
    @Test
    void vlasnikObjavljujeZatvaraPonovnoOtvaraIZapocinjeOdabir() throws Exception {
        String[] targets = {"OPEN", "CLOSED", "OPEN", "CLOSED", "SELECTION"};
        for (int i = 0; i < targets.length; i++) {
            when(clock.instant()).thenReturn(NOW.plusSeconds(60L * (i + 1)));
            changeStatus(opportunityId, targets[i])
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(opportunityId.toString()))
                    .andExpect(jsonPath("$.status").value(targets[i]));
        }

        mockMvc.perform(get(PATH).param("statuses", "SELECTION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(opportunityId.toString()))
                .andExpect(jsonPath("$.data[0].status").value("SELECTION"));
    }

    @Test
    void zatvorenaPrilikaSIsteklimRokomSeNeMozePonovnoOtvoriti() throws Exception {
        changeStatus(opportunityId, "OPEN").andExpect(status().isOk());
        changeStatus(opportunityId, "CLOSED").andExpect(status().isOk());

        // Rok iz opportunityInput je 2026-11-30T10:00:00Z, pa je ovaj trenutak nakon njega.
        when(clock.instant()).thenReturn(Instant.parse("2026-12-01T10:00:00Z"));
        changeStatus(opportunityId, "OPEN")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DEADLINE_PASSED"));

        assertThat(statusOf(opportunityId)).isEqualTo("CLOSED");
    }

    @Test
    void prilikaUOdabiruNeMozeNatragUOtvorenoIliZatvoreno() throws Exception {
        for (String target : new String[] {"OPEN", "CLOSED", "SELECTION"}) {
            changeStatus(opportunityId, target).andExpect(status().isOk());
        }

        for (String target : new String[] {"OPEN", "CLOSED"}) {
            changeStatus(opportunityId, target)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("INVALID_STATUS_TRANSITION"));
            assertThat(statusOf(opportunityId)).isEqualTo("SELECTION");
        }
    }

    @Test
    void drugiOrganizatorNeMozeArhiviratiObrisatiNiMijenjatiStatusTudePrilike() throws Exception {
        changeStatus(opportunityId, "OPEN").andExpect(status().isOk());
        loginAs("ORGANIZER");

        mockMvc.perform(post(PATH + "/" + opportunityId + "/archive").with(withCsrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        mockMvc.perform(delete(PATH + "/" + opportunityId).with(withCsrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        changeStatus(opportunityId, "CLOSED")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        assertThat(jdbc.queryForMap(
                "SELECT status, archived, organizer_id FROM opportunities WHERE id = ?", opportunityId))
                .containsEntry("status", "OPEN")
                .containsEntry("archived", false)
                .containsEntry("organizer_id", ownerId);
    }

    @Test
    void arhiviranaPrilikaJeSamoNaPopisuArhiviranihMojihPrilika() throws Exception {
        UUID activeId = createOpportunity("Aktivna izložba", "Ostaje na aktivnom popisu");
        changeStatus(opportunityId, "OPEN").andExpect(status().isOk());

        mockMvc.perform(post(PATH + "/" + opportunityId + "/archive").with(withCsrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(get("/api/v1/me/opportunities").param("archived", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(opportunityId.toString()))
                .andExpect(jsonPath("$.data[0].archived").value(true))
                .andExpect(jsonPath("$.data[0].status").value("CLOSED"));
        mockMvc.perform(get("/api/v1/me/opportunities").param("archived", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(activeId.toString()));
    }

    @Test
    void vlasnikBriseSvojuPrilikuBezPrijava() throws Exception {
        mockMvc.perform(delete(PATH + "/" + opportunityId).with(withCsrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(PATH + "/" + opportunityId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        mockMvc.perform(get("/api/v1/me/opportunities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // OPP-02-TEST: vidljivost preko pretrage i detalja te filtri javne pretrage.
    // Stanja koja API ne može izravno postaviti (arhiva, skrivanje, status računa) upisuju se u bazu.
    @Test
    void nejavnePrilikeNisuUAnonimnojPretraziAliIhVlasnikIAdministratorOtvaraju() throws Exception {
        UUID archived = insertOpportunity(ownerId, "Arhivirana izložba", "OPEN",
                "2026-12-01T12:00:00+01:00", "Zagreb", artFieldId);
        UUID hidden = insertOpportunity(ownerId, "Skrivena izložba", "OPEN",
                "2026-12-01T12:00:00+01:00", "Zagreb", artFieldId);
        jdbc.update("UPDATE opportunities SET archived = true WHERE id = ?", archived);
        jdbc.update("UPDATE opportunities SET hidden_by_admin = true WHERE id = ?", hidden);
        List<UUID> nonPublic = List.of(opportunityId, archived, hidden);

        mockMvc.perform(get(PATH).with(anonymous())
                        .param("statuses", "OPEN", "CLOSED", "SELECTION", "FINISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(0))
                .andExpect(jsonPath("$.data", empty()));
        for (UUID id : nonPublic) {
            mockMvc.perform(get(PATH + "/" + id).with(anonymous()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }

        loginAsExisting(ownerId);
        for (UUID id : nonPublic) {
            mockMvc.perform(get(PATH + "/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()));
        }

        loginAs("ADMIN");
        for (UUID id : nonPublic) {
            mockMvc.perform(get(PATH + "/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()));
        }
    }

    @Test
    void prilikaBlokiranogOrganizatoraNijeJavnoVidljiva() throws Exception {
        UUID blocked = organizer("BLOCKED");
        UUID id = insertOpportunity(blocked, "Prilika blokiranog organizatora", "OPEN",
                "2026-12-01T12:00:00+01:00", "Zagreb", artFieldId);

        mockMvc.perform(get(PATH).with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", not(hasItem(id.toString()))));
        mockMvc.perform(get(PATH + "/" + id).with(anonymous()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void prilikaObrisanogOrganizatoraJeJavnaSAnonimiziranimImenom() throws Exception {
        UUID deleted = organizer("DELETED");
        UUID id = insertOpportunity(deleted, "Prilika obrisanog organizatora", "OPEN",
                "2026-12-01T12:00:00+01:00", "Zagreb", artFieldId);

        mockMvc.perform(get(PATH + "/" + id).with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizer.displayName").value("Obrisani korisnik"));
        mockMvc.perform(get(PATH).with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", contains(id.toString())))
                .andExpect(jsonPath("$.data[0].organizer.displayName").value("Obrisani korisnik"));
    }

    @Test
    void filtarGradaNeRazlikujeVelikaIMalaSlovaIIzostavljaOnlinePrilike() throws Exception {
        UUID zagreb = insertOpportunity(ownerId, "Zagrebačka izložba", "OPEN",
                "2026-12-01T12:00:00+01:00", "Zagreb", artFieldId);
        UUID zagrebUpper = insertOpportunity(ownerId, "Izložba u centru", "OPEN",
                "2026-12-02T12:00:00+01:00", "ZAGREB", artFieldId);
        insertOpportunity(ownerId, "Splitska izložba", "OPEN", "2026-12-03T12:00:00+01:00", "Split", artFieldId);
        insertOpportunity(ownerId, "Online izložba", "OPEN", "2026-12-04T12:00:00+01:00", null, artFieldId);

        mockMvc.perform(get(PATH).with(anonymous()).param("city", "zagreb"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(2))
                .andExpect(jsonPath("$.data[*].id", contains(zagreb.toString(), zagrebUpper.toString())));
    }

    @Test
    void dvaPodrucjaVracajuPrilikeSBaremJednimBezDuplikata() throws Exception {
        List<UUID> fields = jdbc.queryForList(
                "SELECT id FROM art_fields WHERE active = true ORDER BY position LIMIT 3", UUID.class);
        UUID first = insertOpportunity(ownerId, "Samo prvo područje", "OPEN",
                "2026-12-01T12:00:00+01:00", "Zagreb", fields.get(0));
        UUID second = insertOpportunity(ownerId, "Samo drugo područje", "OPEN",
                "2026-12-02T12:00:00+01:00", "Zagreb", fields.get(1));
        UUID both = insertOpportunity(ownerId, "Oba područja", "OPEN",
                "2026-12-03T12:00:00+01:00", "Zagreb", fields.get(0), fields.get(1));
        insertOpportunity(ownerId, "Treće područje", "OPEN", "2026-12-04T12:00:00+01:00", "Zagreb", fields.get(2));

        mockMvc.perform(get(PATH).with(anonymous())
                        .param("artFieldIds", fields.get(0).toString(), fields.get(1).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(3))
                .andExpect(jsonPath("$.data[*].id",
                        contains(first.toString(), second.toString(), both.toString())));
    }

    @Test
    void istiDatumOdIDoObuhvacaRokKasnoNavecerPoZagrebackomVremenu() throws Exception {
        // 31. 12. u 23:30 u Zagrebu je 22:30 UTC; sljedeći dan u 00:30 više nije taj datum.
        UUID lateEvening = insertOpportunity(ownerId, "Novogodišnja izložba", "OPEN",
                "2026-12-31T23:30:00+01:00", "Zagreb", artFieldId);
        insertOpportunity(ownerId, "Izložba u novoj godini", "OPEN",
                "2027-01-01T00:30:00+01:00", "Zagreb", artFieldId);

        mockMvc.perform(get(PATH).with(anonymous())
                        .param("deadlineFrom", "2026-12-31")
                        .param("deadlineTo", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(1))
                .andExpect(jsonPath("$.data[*].id", contains(lateEvening.toString())))
                .andExpect(jsonPath("$.data[0].deadline").value("2026-12-31T22:30:00Z"));
    }

    @Test
    void kljucnaRijecPretrazujeCijeleRijeciNaslova() throws Exception {
        UUID ceramics = insertOpportunity(ownerId, "Ljetna izložba keramike", "OPEN",
                "2026-12-01T12:00:00+01:00", "Zagreb", artFieldId);

        mockMvc.perform(get(PATH).with(anonymous()).param("q", "keramike"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", contains(ceramics.toString())));
        mockMvc.perform(get(PATH).with(anonymous()).param("q", "slikarstvo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(0))
                .andExpect(jsonPath("$.data", empty()));
    }

    @Test
    void drugaStranicaSilaznoPoRokuJeStabilna() throws Exception {
        record Row(UUID id, OffsetDateTime deadline) {}
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            // Parovi prilika dijele rok, pa poredak unutar para određuje ID.
            var deadline = OffsetDateTime.parse("2026-12-01T12:00:00+01:00").plusDays(i / 2);
            rows.add(new Row(insertOpportunity(ownerId, "Prilika " + i, "OPEN",
                    deadline.toString(), "Zagreb", artFieldId), deadline));
        }
        String[] expected = rows.stream()
                // PostgreSQL uspoređuje UUID bajt po bajt, kao mali heksadecimalni zapis;
                // UUID.compareTo bi uspoređivao predznačene long vrijednosti.
                .sorted(Comparator.comparing(Row::deadline).reversed()
                        .thenComparing(row -> row.id().toString()))
                .skip(10).limit(10)
                .map(row -> row.id().toString())
                .toArray(String[]::new);

        for (int attempt = 0; attempt < 2; attempt++) {
            mockMvc.perform(get(PATH).with(anonymous())
                            .param("page", "2").param("pageSize", "10").param("sort", "-deadline"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.meta.page").value(2))
                    .andExpect(jsonPath("$.meta.pageSize").value(10))
                    .andExpect(jsonPath("$.meta.total").value(25))
                    .andExpect(jsonPath("$.data[*].id", contains(expected)));
        }
    }

    private UUID organizer(String userStatus) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, email, display_name, role, status, organizer_approval) "
                + "VALUES (?, ?, 'Stvarno ime', 'ORGANIZER', ?, 'APPROVED')",
                id, id + "@artconnect.test", userStatus);
        return id;
    }

    private void loginAsExisting(UUID id) {
        TestSecurityContextHolder.setAuthentication(
                TestPrincipals.authentication(users.findById(id).orElseThrow()));
    }

    /** Objavljena prilika; grad {@code null} znači online lokaciju. */
    private UUID insertOpportunity(UUID owner, String title, String opportunityStatus, String deadline,
            String city, UUID... fields) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO opportunities (id, organizer_id, type_id, title, description, deadline, "
                + "location_type, online_url, status, published_at) "
                + "VALUES (?, ?, ?, ?, 'Opis prilike', ?::timestamptz, 'ONLINE', 'https://example.org', ?, "
                + "'2026-10-01T10:00:00Z')",
                id, owner, typeId, title, deadline, opportunityStatus);
        if (city != null) {
            jdbc.update("UPDATE opportunities SET location_type = 'PHYSICAL', online_url = NULL, "
                    + "street_address = 'Ilica 1', city = ?, postal_code = '10000', "
                    + "latitude = 45.815, longitude = 15.9819 WHERE id = ?", city, id);
        }
        for (UUID field : fields) {
            jdbc.update("INSERT INTO opportunity_art_fields (opportunity_id, art_field_id) VALUES (?, ?)",
                    id, field);
        }
        return id;
    }

    private UUID createOpportunity(String title, String description) throws Exception {
        var response = mockMvc.perform(post(PATH)
                        .with(withCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(opportunityInput(title, description)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PREPARATION"))
                .andReturn()
                .getResponse();
        return UUID.fromString(
                objectMapper.readTree(response.getContentAsString()).get("id").asString());
    }

    private ResultActions changeStatus(UUID id, String target) throws Exception {
        return mockMvc.perform(post(PATH + "/" + id + "/status")
                .with(withCsrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + target + "\"}"));
    }

    private String statusOf(UUID id) {
        return jdbc.queryForObject("SELECT status FROM opportunities WHERE id = ?", String.class, id);
    }

    private String opportunityInput(String title, String description) {
        return """
                {
                  "title": "%s",
                  "description": "%s",
                  "typeId": "%s",
                  "artFieldIds": ["%s"],
                  "deadline": "2026-11-30T10:00:00Z",
                  "location": {
                    "type": "ONLINE",
                    "onlineUrl": "https://example.org/event"
                  }
                }
                """.formatted(title, description, typeId, artFieldId);
    }
}
