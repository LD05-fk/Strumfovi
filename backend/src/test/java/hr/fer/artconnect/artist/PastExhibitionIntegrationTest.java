package hr.fer.artconnect.artist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.common.TimeConfiguration;
import hr.fer.artconnect.support.IntegrationTestBase;
import java.time.Year;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

class PastExhibitionIntegrationTest extends IntegrationTestBase {
    private static final String PATH = "/api/v1/past-exhibitions";
    private static final int NEXT_YEAR = Year.now(TimeConfiguration.APP_ZONE).getValue() + 1;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void dodajeIzlozbuPrijavljenomUmjetnikuIVracaLocation() throws Exception {
        UUID artistId = loginAsArtistWithProfile();

        String response = send(body("  Gradski motivi  ", 2024, "  Galerija Nova, Zagreb  "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                // Tekstovi se obrezuju prije pohrane.
                .andExpect(jsonPath("$.title").value("Gradski motivi"))
                .andExpect(jsonPath("$.year").value(2024))
                .andExpect(jsonPath("$.place").value("Galerija Nova, Zagreb"))
                .andReturn().getResponse().getContentAsString();

        String id = response.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM past_exhibitions");
        assertThat(row.get("id")).hasToString(id);
        assertThat(row.get("artist_id")).hasToString(artistId.toString());
        assertThat(row.get("title")).isEqualTo("Gradski motivi");
        assertThat(((Number) row.get("year")).intValue()).isEqualTo(2024);
        assertThat(row.get("place")).isEqualTo("Galerija Nova, Zagreb");
        assertThat(row.get("created_at")).isNotNull();
    }

    @Test
    void prihvacaDonjuGranicuGodineITekucuGodinu() throws Exception {
        loginAsArtistWithProfile();

        send(body("Rani rad", 1900, "Zagreb")).andExpect(status().isCreated());
        send(body("Novi rad", NEXT_YEAR - 1, "Zagreb")).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM past_exhibitions", Long.class)).isEqualTo(2);
    }

    @ParameterizedTest
    @CsvSource(value = {
            "NULL | 2024 | Zagreb | title",
            "'' | 2024 | Zagreb | title",
            "'    ' | 2024 | Zagreb | title",
            "Naslov | 2024 | NULL | place",
            "Naslov | 2024 | '   ' | place",
            "Naslov | NULL | Zagreb | year",
            "Naslov | 1899 | Zagreb | year",
            "Naslov | 0 | Zagreb | year"
    }, delimiter = '|', nullValues = "NULL")
    void odbijaNeispravnoTijeloSPoljemUDetaljima(String title, Integer year, String place, String field)
            throws Exception {
        loginAsArtistWithProfile();

        send(body(title, year, place))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value(field.strip()));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM past_exhibitions", Long.class)).isZero();
    }

    @ParameterizedTest
    @CsvSource({"title", "place"})
    void odbijaTekstDuziOd200Znakova(String field) throws Exception {
        loginAsArtistWithProfile();
        String tooLong = "x".repeat(201);

        send(body("title".equals(field) ? tooLong : "Naslov", 2024, "place".equals(field) ? tooLong : "Zagreb"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value(field));
    }

    @Test
    void odbijaGodinuUBuducnostiSPoljemYear() throws Exception {
        loginAsArtistWithProfile();

        send(body("Gradski motivi", NEXT_YEAR, "Zagreb"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("year"))
                .andExpect(jsonPath("$.error.details[0].issue")
                        .value("Godina ne smije biti u budućnosti."));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM past_exhibitions", Long.class)).isZero();
    }

    @Test
    void neprijavljenomVracaUnauthenticated() throws Exception {
        mockMvc.perform(post(PATH).with(anonymous()).with(withCsrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body("Gradski motivi", 2024, "Zagreb")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void organizatoruVracaForbidden() throws Exception {
        loginAs("ORGANIZER");

        send(body("Gradski motivi", 2024, "Zagreb"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void bezCsrfTokenaVracaForbidden() throws Exception {
        loginAsArtistWithProfile();

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Gradski motivi", 2024, "Zagreb")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void umjetnikuBezProfilaStvaraPrazanProfilIDodajeIzlozbu() throws Exception {
        UUID artistId = loginAs("ARTIST");

        send(body("Gradski motivi", 2024, "Zagreb")).andExpect(status().isCreated());

        Map<String, Object> profile = jdbc.queryForMap(
                "SELECT * FROM artist_profiles WHERE user_id = ?", artistId);
        assertThat(profile.get("biography")).isNull();
        assertThat(profile.get("artist_statement")).isNull();
        assertThat(profile.get("updated_at")).isNotNull();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM past_exhibitions WHERE artist_id = ?", Long.class, artistId))
                .isEqualTo(1);
    }

    @Test
    void azuriraVlastituIzlozbuIObrezujeTekst() throws Exception {
        UUID artistId = loginAsArtistWithProfile();
        UUID exhibitionId = insertExhibition(artistId, "Stari naslov", 2020, "Split");

        sendUpdate(exhibitionId, body("  Gradski motivi  ", 2024, "  Galerija Nova, Zagreb  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(exhibitionId.toString()))
                .andExpect(jsonPath("$.title").value("Gradski motivi"))
                .andExpect(jsonPath("$.year").value(2024))
                .andExpect(jsonPath("$.place").value("Galerija Nova, Zagreb"));

        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM past_exhibitions WHERE id = ?", exhibitionId);
        assertThat(row.get("title")).isEqualTo("Gradski motivi");
        assertThat(((Number) row.get("year")).intValue()).isEqualTo(2024);
        assertThat(row.get("place")).isEqualTo("Galerija Nova, Zagreb");
    }

    @Test
    void odbijaNeispravnoTijeloPriAzuriranju() throws Exception {
        UUID artistId = loginAsArtistWithProfile();
        UUID exhibitionId = insertExhibition(artistId, "Stari naslov", 2020, "Split");

        sendUpdate(exhibitionId, body("   ", 2024, "Zagreb"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("title"));
        assertThat(title(exhibitionId)).isEqualTo("Stari naslov");
    }

    @Test
    void neprijavljenomPriAzuriranjuVracaUnauthenticated() throws Exception {
        mockMvc.perform(put(PATH + "/" + UUID.randomUUID()).with(anonymous()).with(withCsrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body("Gradski motivi", 2024, "Zagreb")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void organizatoruPriAzuriranjuVracaForbidden() throws Exception {
        loginAs("ORGANIZER");

        sendUpdate(UUID.randomUUID(), body("Gradski motivi", 2024, "Zagreb"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void tudjuIzlozbuTretiraKaoNepostojecu() throws Exception {
        UUID vlasnik = loginAsArtistWithProfile();
        UUID exhibitionId = insertExhibition(vlasnik, "Stari naslov", 2020, "Split");
        // Druga prijava zamjenjuje prvu: zahtjev sada šalje drugi umjetnik.
        loginAsArtistWithProfile();

        sendUpdate(exhibitionId, body("Gradski motivi", 2024, "Zagreb"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        assertThat(title(exhibitionId)).isEqualTo("Stari naslov");
    }

    @Test
    void nepostojecuIzlozbuVracaNotFound() throws Exception {
        loginAsArtistWithProfile();

        sendUpdate(UUID.randomUUID(), body("Gradski motivi", 2024, "Zagreb"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    private ResultActions send(String body) throws Exception {
        return mockMvc.perform(post(PATH).with(withCsrf())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions sendUpdate(UUID exhibitionId, String body) throws Exception {
        return mockMvc.perform(put(PATH + "/" + exhibitionId).with(withCsrf())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private UUID insertExhibition(UUID artistId, String title, int year, String place) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO past_exhibitions (id, artist_id, title, year, place) VALUES (?, ?, ?, ?, ?)",
                id, artistId, title, year, place);
        return id;
    }

    private String title(UUID exhibitionId) {
        return jdbc.queryForObject("SELECT title FROM past_exhibitions WHERE id = ?", String.class, exhibitionId);
    }

    // Umjetnik koji je već spremio profil ima svoj red u artist_profiles; endpoint ga tada
    // samo dohvaća, pa ga test ubacuje sam.
    private UUID loginAsArtistWithProfile() {
        UUID artistId = loginAs("ARTIST");
        jdbc.update("INSERT INTO artist_profiles (user_id) VALUES (?)", artistId);
        return artistId;
    }

    private static String body(String title, Integer year, String place) {
        return "{\"title\":" + json(title) + ",\"year\":" + (year == null ? "null" : year)
                + ",\"place\":" + json(place) + "}";
    }

    private static String json(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
