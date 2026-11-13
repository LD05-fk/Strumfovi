package hr.fer.artconnect.opportunity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.support.IntegrationTestBase;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Javna pretraga na 2000 prilika: upiti pretrage koriste predviđene indekse i izvode se brzo.
 *
 * SQL se ne prepisuje u test, nego se uhvati iz {@link JdbcClient} dok API izvodi pretragu, pa
 * promjena upita u repozitoriju koja onemogući indeks ruši ovaj test.
 *
 * Pretraga izvodi dva upita. Brojanje ({@code COUNT(*)}) nema sortiranja, pa filtar po ključnoj
 * riječi ili gradu ide kroz {@code ix_opp_fts} odnosno {@code ix_opp_city_lower}. Stranica
 * ({@code ORDER BY deadline LIMIT 20}) čita {@code ix_opp_public} već poredan po roku i staje
 * nakon 20 pogodaka; na ovoj količini podataka planer taj put bira i kad je zadan filtar.
 */
class OpportunitySearchPerformanceIT extends IntegrationTestBase {
    private static final int OPPORTUNITIES = 2000;
    private static final long MAX_EXECUTION_MS = 200;
    private static final Pattern EXECUTION_TIME = Pattern.compile("Execution Time: ([0-9.]+) ms");
    private static final List<String> CITIES = List.of(
            "Zagreb", "Split", "Rijeka", "Osijek", "Zadar", "Pula", "Slavonski Brod", "Karlovac",
            "Varaždin", "Šibenik", "Sisak", "Velika Gorica", "Dubrovnik", "Bjelovar", "Koprivnica",
            "Vinkovci", "Vukovar", "Čakovec", "Požega", "Samobor", "Virovitica", "Gospić", "Krapina",
            "Pazin", "Poreč", "Rovinj", "Makarska", "Trogir", "Omiš", "Sinj", "Knin", "Đakovo",
            "Našice", "Križevci", "Ogulin", "Opatija", "Crikvenica", "Labin", "Umag", "Metković");
    private static final List<String> WORDS = List.of(
            "natječaj", "izložba", "rezidencija", "slikarstvo", "fotografija", "skulptura", "grafika",
            "instalacija", "ilustracija", "mural", "video", "zvuk", "performans", "dizajn", "tekstil");

    @Autowired JdbcTemplate jdbc;
    @Autowired NamedParameterJdbcTemplate namedJdbc;
    @MockitoSpyBean JdbcClient jdbcClient;

    @BeforeEach
    void twoThousandOpportunities() {
        // Fiksno sjeme daje iste podatke u svakom izvođenju.
        Random random = new Random(20261113);
        List<UUID> organizers = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            UUID id = UUID.randomUUID();
            jdbc.update("INSERT INTO users (id, email, display_name, role, status, organizer_approval) "
                    + "VALUES (?, ?, ?, 'ORGANIZER', ?, 'APPROVED')",
                    id, "perf" + i + "@artconnect.test", "Organizator " + i, i < 2 ? "BLOCKED" : "ACTIVE");
            organizers.add(id);
        }
        List<UUID> types = jdbc.queryForList("SELECT id FROM opportunity_types WHERE active = true", UUID.class);
        List<UUID> fields = jdbc.queryForList("SELECT id FROM art_fields WHERE active = true", UUID.class);
        String[] statuses = {"OPEN", "OPEN", "OPEN", "OPEN", "OPEN", "CLOSED", "CLOSED", "SELECTION",
                "FINISHED", "PREPARATION"};
        Instant base = Instant.parse("2026-11-01T10:00:00Z");

        List<Object[]> opportunities = new ArrayList<>();
        List<Object[]> links = new ArrayList<>();
        for (int i = 0; i < OPPORTUNITIES; i++) {
            UUID id = UUID.randomUUID();
            // Svaka stota prilika je izložba keramike; ostali naslovi su kombinacije čestih riječi.
            String title = i % 100 == 0 ? "Ljetna izložba keramike " + i
                    : WORDS.get(random.nextInt(WORDS.size())) + " " + WORDS.get(random.nextInt(WORDS.size()))
                            + " " + i;
            boolean online = random.nextInt(5) == 0;
            String status = statuses[random.nextInt(statuses.length)];
            Timestamp deadline = Timestamp.from(base.plus(random.nextInt(365 * 24), ChronoUnit.HOURS));
            opportunities.add(new Object[] {
                    id, organizers.get(random.nextInt(organizers.size())),
                    types.get(random.nextInt(types.size())), title,
                    "Opis prilike " + i + " " + WORDS.get(random.nextInt(WORDS.size())),
                    deadline, online ? "ONLINE" : "PHYSICAL", online ? "https://example.org" : null,
                    online ? null : "Ilica 1", online ? null : CITIES.get(random.nextInt(CITIES.size())),
                    online ? null : 45.815, online ? null : 15.9819, status,
                    "PREPARATION".equals(status) ? null : Timestamp.from(base),
                    random.nextInt(20) == 0, random.nextInt(30) == 0});
            links.add(new Object[] {id, fields.get(random.nextInt(fields.size()))});
        }
        jdbc.batchUpdate("INSERT INTO opportunities (id, organizer_id, type_id, title, description, deadline, "
                + "location_type, online_url, street_address, city, latitude, longitude, status, published_at, "
                + "archived, hidden_by_admin) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                opportunities);
        jdbc.batchUpdate("INSERT INTO opportunity_art_fields (opportunity_id, art_field_id) VALUES (?, ?)", links);
        jdbc.execute("ANALYZE opportunities");
        jdbc.execute("ANALYZE opportunity_art_fields");
        jdbc.execute("ANALYZE users");
    }

    @ParameterizedTest(name = "{0}: upit {2} koristi {3}")
    @CsvSource({
            "ključna riječ, q=keramike, COUNT, ix_opp_fts",
            "grad, city=Zagreb, COUNT, ix_opp_city_lower",
            "zadano sortiranje, '', PAGE, ix_opp_public",
    })
    void upitPretrageKoristiIndeksIBrzJe(String scenario, String query, String indexedQuery, String index)
            throws Exception {
        clearInvocations(jdbcClient);
        MockHttpServletRequestBuilder request = get("/api/v1/opportunities").with(anonymous());
        if (!query.isEmpty()) {
            String[] pair = query.split("=", 2);
            request = request.param(pair[0], pair[1]);
        }
        mockMvc.perform(request).andExpect(status().isOk());

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbcClient, atLeastOnce()).sql(sql.capture());
        String count = sql.getAllValues().stream().filter(s -> s.contains("COUNT(*)")).findFirst().orElseThrow();
        String page = sql.getAllValues().stream().filter(s -> s.contains("LIMIT :limit")).findFirst().orElseThrow();

        // Iste vrijednosti kakve servis predaje repozitoriju za ovaj upit.
        Map<String, Object> params = Map.of(
                "statuses", List.of("OPEN"), "q", "keramike", "city", "Zagreb", "limit", 20, "offset", 0L);
        String pagePlan = explainAnalyze(page, params);
        String countPlan = explainAnalyze(count, params);
        assertThat("PAGE".equals(indexedQuery) ? pagePlan : countPlan)
                .as("plan upita %s za %s", indexedQuery, scenario).contains(index);
        assertThat(executionMs(pagePlan)).as("trajanje stranice za %s", scenario).isLessThan(MAX_EXECUTION_MS);
        assertThat(executionMs(countPlan)).as("trajanje brojanja za %s", scenario).isLessThan(MAX_EXECUTION_MS);
    }

    private String explainAnalyze(String sql, Map<String, Object> params) {
        return String.join("\n", namedJdbc.queryForList("EXPLAIN (ANALYZE) " + sql, params, String.class));
    }

    private static double executionMs(String plan) {
        Matcher matcher = EXECUTION_TIME.matcher(plan);
        assertThat(matcher.find()).as("EXPLAIN ANALYZE bez vremena izvođenja:%n%s", plan).isTrue();
        return Double.parseDouble(matcher.group(1));
    }
}
