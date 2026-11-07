package hr.fer.artconnect.opportunity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.auth.domain.UserRepository;
import hr.fer.artconnect.support.IntegrationTestBase;
import hr.fer.artconnect.support.TestPrincipals;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;
import java.util.stream.Stream;

/**
 * Javna pretraga prilika: GET /api/v1/opportunities
 *
 * Skup prilika postavlja se izravno u bazu jer testovi provjeravaju filtriranje i vidljivost,
 * a ne stvaranje prilike — statusi, arhiva i skrivanje inače traže više koraka kroz API.
 */
class OpportunitySearchIntegrationTest extends IntegrationTestBase {
    private static final String PATH = "/api/v1/opportunities";

    private static final String SLIKARI = "Natječaj za slikare";
    private static final String FOTOGRAFIJA = "Izložba fotografije";
    private static final String SKULPTURA_ONLINE = "Online natječaj skulptura";
    private static final String ZATVORENA = "Zatvorena izložba";

    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @MockitoSpyBean JdbcClient jdbcClient;

    private UUID organizer;
    private final Map<String, UUID> catalog = new LinkedHashMap<>();

    @BeforeEach
    void dataset() {
        organizer = organizer("galerija@artconnect.test", "ACTIVE");
        jdbc.update("INSERT INTO organizer_profiles (user_id, organization_name) VALUES (?, 'Galerija Nova')",
                organizer);
        UUID blocked = organizer("blokiran@artconnect.test", "BLOCKED");

        List<UUID> types = jdbc.queryForList(
                "SELECT id FROM opportunity_types WHERE active = true ORDER BY position LIMIT 2", UUID.class);
        List<UUID> fields = jdbc.queryForList(
                "SELECT id FROM art_fields WHERE active = true ORDER BY position LIMIT 3", UUID.class);
        catalog.put("t1", types.get(0));
        catalog.put("t2", types.get(1));
        catalog.put("f1", fields.get(0));
        catalog.put("f2", fields.get(1));
        catalog.put("f3", fields.get(2));

        // Javno vidljive prilike.
        physical(organizer, SLIKARI, "Pozivamo mlade slikare na natječaj.", "t1", "Zagreb",
                "2026-12-01T12:00:00+01:00", "OPEN", "2026-10-01T10:00:00Z", "f1");
        physical(organizer, FOTOGRAFIJA, "Skupna izložba fotografije.", "t2", "Split",
                "2027-01-15T12:00:00+01:00", "OPEN", "2026-10-02T10:00:00Z", "f2");
        online(organizer, SKULPTURA_ONLINE, "Natječaj za skulpture i slike, prijave mrežno.", "t1",
                "2027-02-20T12:00:00+01:00", "OPEN", "2026-10-03T10:00:00Z", "f3", "f1");
        physical(organizer, ZATVORENA, "Prijave su zatvorene.", "t2", "Zagreb",
                "2027-03-01T12:00:00+01:00", "CLOSED", "2026-10-04T10:00:00Z", "f2");

        // Prilike koje javna pretraga nikad ne smije vratiti.
        physical(organizer, "Nacrt prilike", "Još nije objavljena.", "t1", "Zagreb",
                "2027-04-01T12:00:00+01:00", "PREPARATION", null, "f1");
        hide(physical(organizer, "Arhivirana prilika", "Arhiva.", "t1", "Zagreb",
                "2027-04-02T12:00:00+01:00", "OPEN", "2026-10-05T10:00:00Z", "f1"), "archived");
        hide(physical(organizer, "Skrivena prilika", "Skrio administrator.", "t1", "Zagreb",
                "2027-04-03T12:00:00+01:00", "OPEN", "2026-10-06T10:00:00Z", "f1"), "hidden_by_admin");
        physical(blocked, "Prilika blokiranog organizatora", "Organizator je blokiran.", "t1", "Zagreb",
                "2027-04-04T12:00:00+01:00", "OPEN", "2026-10-07T10:00:00Z", "f1");
    }

    @Test
    void anonimniKorisnikDobivaSamoOtvorenePrilikeSPunimSazetkom() throws Exception {
        search("").andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.pageSize").value(20))
                .andExpect(jsonPath("$.meta.total").value(3))
                .andExpect(jsonPath("$.data", hasSize(3)))
                // Zadani poredak je rok uzlazno.
                .andExpect(jsonPath("$.data[*].title", contains(SLIKARI, FOTOGRAFIJA, SKULPTURA_ONLINE)))
                .andExpect(jsonPath("$.data[0].status").value("OPEN"))
                .andExpect(jsonPath("$.data[0].deadline").value("2026-12-01T11:00:00Z"))
                .andExpect(jsonPath("$.data[0].type.id").value(catalog.get("t1").toString()))
                .andExpect(jsonPath("$.data[0].artFields[0].id").value(catalog.get("f1").toString()))
                .andExpect(jsonPath("$.data[0].locationType").value("PHYSICAL"))
                .andExpect(jsonPath("$.data[0].city").value("Zagreb"))
                .andExpect(jsonPath("$.data[0].latitude").value(45.815))
                .andExpect(jsonPath("$.data[0].longitude").value(15.9819))
                .andExpect(jsonPath("$.data[0].eventStart").value(nullValue()))
                .andExpect(jsonPath("$.data[0].organizer.id").value(organizer.toString()))
                .andExpect(jsonPath("$.data[0].organizer.organizationName").value("Galerija Nova"))
                // Online prilika nema grad ni koordinate, a sažetak ne nosi opis ni uvjete.
                .andExpect(jsonPath("$.data[2].locationType").value("ONLINE"))
                .andExpect(jsonPath("$.data[2].city").value(nullValue()))
                .andExpect(jsonPath("$.data[2].latitude").value(nullValue()))
                .andExpect(jsonPath("$.data[2].description").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ANONIMNO", "VLASNIK", "ADMIN"})
    void nejavnePrilikeNeVidiNiVlasnikNiAdministrator(String viewer) throws Exception {
        switch (viewer) {
            case "VLASNIK" -> loginAsExisting(organizer);
            case "ADMIN" -> loginAs("ADMIN");
            default -> TestSecurityContextHolder.clearContext();
        }

        search("?statuses=OPEN&statuses=CLOSED&statuses=SELECTION&statuses=FINISHED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(4))
                .andExpect(jsonPath("$.data[*].title",
                        containsInAnyOrder(SLIKARI, FOTOGRAFIJA, SKULPTURA_ONLINE, ZATVORENA)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("filtri")
    void filtarVracaOcekivanePrilike(String query, List<String> expected) throws Exception {
        var result = search(query).andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(expected.size()));
        if (expected.isEmpty()) {
            result.andExpect(jsonPath("$.data", empty()));
        } else {
            result.andExpect(jsonPath("$.data[*].title", containsInAnyOrder(expected.toArray())));
        }
    }

    static Stream<Arguments> filtri() {
        return Stream.of(
                // Ključna riječ pretražuje naslov i opis.
                Arguments.of("?q=slikare", List.of(SLIKARI)),
                Arguments.of("?q=natječaj", List.of(SLIKARI, SKULPTURA_ONLINE)),
                Arguments.of("?q=fotografije", List.of(FOTOGRAFIJA)),
                Arguments.of("?q=slikar", List.of()),
                Arguments.of("?q=%20%20%20", List.of(SLIKARI, FOTOGRAFIJA, SKULPTURA_ONLINE)),
                Arguments.of("?typeIds={t1}", List.of(SLIKARI, SKULPTURA_ONLINE)),
                Arguments.of("?typeIds={t1}&typeIds={t2}", List.of(SLIKARI, FOTOGRAFIJA, SKULPTURA_ONLINE)),
                // Prilika zadovoljava filtar ako ima barem jedno od navedenih područja.
                Arguments.of("?artFieldIds={f1}", List.of(SLIKARI, SKULPTURA_ONLINE)),
                Arguments.of("?artFieldIds={f2}&artFieldIds={f3}", List.of(FOTOGRAFIJA, SKULPTURA_ONLINE)),
                // Grad je točno podudaranje bez obzira na velika i mala slova, online prilike ispadaju.
                Arguments.of("?city=zagreb", List.of(SLIKARI)),
                Arguments.of("?city=%20Split%20", List.of(FOTOGRAFIJA)),
                Arguments.of("?city=Zag", List.of()),
                Arguments.of("?locationType=ONLINE", List.of(SKULPTURA_ONLINE)),
                Arguments.of("?locationType=PHYSICAL", List.of(SLIKARI, FOTOGRAFIJA)),
                Arguments.of("?statuses=CLOSED", List.of(ZATVORENA)),
                Arguments.of("?statuses=FINISHED", List.of()),
                // Granice raspona su cijeli dani u zoni Europe/Zagreb, obje uključene.
                Arguments.of("?deadlineFrom=2026-12-01&deadlineTo=2026-12-01", List.of(SLIKARI)),
                Arguments.of("?deadlineTo=2026-11-30", List.of()),
                Arguments.of("?deadlineFrom=2027-01-01", List.of(FOTOGRAFIJA, SKULPTURA_ONLINE)),
                // Filtri se kombiniraju konjunkcijom.
                Arguments.of("?typeIds={t1}&locationType=PHYSICAL", List.of(SLIKARI)),
                Arguments.of("?q=natječaj&artFieldIds={f3}", List.of(SKULPTURA_ONLINE)),
                Arguments.of("?statuses=CLOSED&city=Zagreb", List.of(ZATVORENA)),
                Arguments.of("?city=Zagreb&locationType=ONLINE", List.of()),
                // Nepostojeća stavka šifrarnika nije greška, nego prazan rezultat.
                Arguments.of("?typeIds=00000000-0000-4000-8000-000000000000", List.of()),
                Arguments.of("?artFieldIds=00000000-0000-4000-8000-000000000000", List.of()));
    }

    @Test
    void sortiranjeIStranicenjeDajuStabilanRedoslijed() throws Exception {
        search("?sort=-deadline").andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].title", contains(SKULPTURA_ONLINE, FOTOGRAFIJA, SLIKARI)));
        search("?sort=-publishedAt").andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].title", contains(SKULPTURA_ONLINE, FOTOGRAFIJA, SLIKARI)));

        search("?pageSize=2&page=1").andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(3))
                .andExpect(jsonPath("$.data[*].title", contains(SLIKARI, FOTOGRAFIJA)));
        search("?pageSize=2&page=2").andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.page").value(2))
                .andExpect(jsonPath("$.meta.total").value(3))
                .andExpect(jsonPath("$.data[*].title", contains(SKULPTURA_ONLINE)));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            // PREPARATION nije javni status, pa je filtar po njemu neispravan zahtjev.
            "?statuses=PREPARATION, statuses",
            "?statuses=NEPOSTOJECI, statuses",
            "?deadlineFrom=2027-03-01&deadlineTo=2027-01-01, deadlineFrom",
            "?deadlineFrom=prekosutra, deadlineFrom",
            "?typeIds=nije-uuid, typeIds",
            "?locationType=NEGDJE, locationType",
            "?pageSize=101, pageSize",
            "?page=0, page",
            "?sort=title, sort",
    })
    void neispravanParametarVraca400SPoljem(String query, String field) throws Exception {
        search(query).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[*].field", hasItem(field)));
    }

    @Test
    void predugaKljucnaRijecVraca400() throws Exception {
        search("?q=" + "a".repeat(101)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[*].field", hasItem("q")));
    }

    @Test
    void podrucjaUmjetnostiStraniceDohvacaJedanUpit() throws Exception {
        clearInvocations(jdbcClient);

        search("").andExpect(status().isOk())
                // Područja su stvarno popunjena, uključujući priliku s dva područja.
                .andExpect(jsonPath("$.data[0].artFields", hasSize(1)))
                .andExpect(jsonPath("$.data[1].artFields", hasSize(1)))
                .andExpect(jsonPath("$.data[2].artFields", hasSize(2)));

        // Tri prilike na stranici, a ne jedan upit za područja po prilici:
        // COUNT(*), stranica prilika i jedan upit za sva područja stranice.
        verify(jdbcClient, times(3)).sql(anyString());
    }

    @Test
    void upitiPretrageMoguKoristitiPredvideneIndekse() {
        // Plan se traži uz isključen sekvencijalni prolaz jer je ispitni skup premali da ga planer
        // sam odbaci. Test čuva oblik izraza: indeks se može koristiti samo ako je izraz upita
        // identičan izrazu indeksa (ix_opp_fts) i ako uvjeti vidljivosti doslovno postoje (ix_opp_public).
        jdbc.execute("SET enable_seqscan = off");
        try {
            assertThat(plan("SELECT id FROM opportunities WHERE to_tsvector('simple', title || ' ' || description)"
                    + " @@ plainto_tsquery('simple', 'natjecaj')")).contains("ix_opp_fts");
            // Uz više statusa planer ne može posegnuti za užim djelomičnim indeksom ix_opp_open_deadline,
            // koji inače služi zadanoj pretrazi po statusu OPEN.
            assertThat(plan("SELECT id FROM opportunities WHERE archived = false AND hidden_by_admin = false"
                    + " AND status IN ('OPEN', 'CLOSED') ORDER BY deadline")).contains("ix_opp_public");
        } finally {
            jdbc.execute("RESET enable_seqscan");
        }
    }

    private String plan(String sql) {
        return String.join("\n", jdbc.queryForList("EXPLAIN " + sql, String.class));
    }

    // Parametri se predaju pojedinačno jer MockMvc ne dekodira upitni dio URL-a.
    private ResultActions search(String query) throws Exception {
        String resolved = query;
        for (var entry : catalog.entrySet()) {
            resolved = resolved.replace("{" + entry.getKey() + "}", entry.getValue().toString());
        }
        var request = get(PATH);
        for (String pair : resolved.replaceFirst("^\\?", "").split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            String[] parts = pair.split("=", 2);
            request = request.param(parts[0],
                    parts.length == 2 ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "");
        }
        return mockMvc.perform(TestSecurityContextHolder.getContext().getAuthentication() == null
                ? request.with(anonymous())
                : request);
    }

    private void loginAsExisting(UUID id) {
        TestSecurityContextHolder.setAuthentication(
                TestPrincipals.authentication(users.findById(id).orElseThrow()));
    }

    private UUID organizer(String email, String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, email, display_name, role, status, organizer_approval) "
                + "VALUES (?, ?, 'Organizator', 'ORGANIZER', ?, 'APPROVED')", id, email, status);
        return id;
    }

    private UUID physical(UUID owner, String title, String description, String type, String city,
            String deadline, String status, String publishedAt, String... fields) {
        UUID id = insert(owner, title, description, type, deadline, status, publishedAt);
        jdbc.update("UPDATE opportunities SET location_type = 'PHYSICAL', street_address = 'Ilica 1', "
                + "city = ?, postal_code = '10000', latitude = 45.815, longitude = 15.9819 WHERE id = ?", city, id);
        link(id, fields);
        return id;
    }

    private UUID online(UUID owner, String title, String description, String type,
            String deadline, String status, String publishedAt, String... fields) {
        UUID id = insert(owner, title, description, type, deadline, status, publishedAt);
        jdbc.update("UPDATE opportunities SET location_type = 'ONLINE', online_url = 'https://example.org' "
                + "WHERE id = ?", id);
        link(id, fields);
        return id;
    }

    private UUID insert(UUID owner, String title, String description, String type,
            String deadline, String status, String publishedAt) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO opportunities (id, organizer_id, type_id, title, description, deadline, "
                + "location_type, online_url, status, published_at) "
                + "VALUES (?, ?, ?, ?, ?, ?::timestamptz, 'ONLINE', 'https://example.org', ?, ?::timestamptz)",
                id, owner, catalog.get(type), title, description, deadline, status, publishedAt);
        return id;
    }

    private void link(UUID id, String... fields) {
        for (String field : fields) {
            jdbc.update("INSERT INTO opportunity_art_fields (opportunity_id, art_field_id) VALUES (?, ?)",
                    id, catalog.get(field));
        }
    }

    private UUID hide(UUID id, String column) {
        jdbc.update("UPDATE opportunities SET " + column + " = true WHERE id = ?", id);
        return id;
    }
}
