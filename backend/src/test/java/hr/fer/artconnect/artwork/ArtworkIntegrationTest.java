package hr.fer.artconnect.artwork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

import hr.fer.artconnect.common.TimeConfiguration;
import hr.fer.artconnect.support.IntegrationTestBase;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

class ArtworkIntegrationTest extends IntegrationTestBase {
    private static final String PATH = "/api/v1/artworks";
    private static final int CURRENT_YEAR = Year.now(TimeConfiguration.APP_ZONE).getValue();
    private static final int NEXT_YEAR = CURRENT_YEAR + 1;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void stvaraDjeloZaPrijavljenogUmjetnikaIVracaLocationBezSlika() throws Exception {
        UUID artistId = loginAs("ARTIST");

        var result = send(body("  Morska tišina  ", 2024, "  Ulje na platnu  ",
                "  ", "  ", "PUBLIC"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.artistId").value(artistId.toString()))
                .andExpect(jsonPath("$.title").value("Morska tišina"))
                .andExpect(jsonPath("$.year").value(2024))
                .andExpect(jsonPath("$.technique").value("Ulje na platnu"))
                .andExpect(jsonPath("$.dimensions").value(nullValue()))
                .andExpect(jsonPath("$.description").value(nullValue()))
                .andExpect(jsonPath("$.visibility").value("PUBLIC"))
                .andExpect(jsonPath("$.hiddenByAdmin").value(false))
                .andExpect(jsonPath("$.images").isEmpty())
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        String id = response.replaceAll(".*\"id\"\\s*:\\s*\"([^\"]+)\".*", "$1");
        assertThat(result.getResponse().getHeader("Location")).isEqualTo(PATH + "/" + id);
        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM artworks");
        assertThat(row.get("id")).hasToString(id);
        assertThat(row.get("artist_id")).hasToString(artistId.toString());
        assertThat(row.get("title")).isEqualTo("Morska tišina");
        assertThat(((Number) row.get("year")).intValue()).isEqualTo(2024);
        assertThat(row.get("technique")).isEqualTo("Ulje na platnu");
        assertThat(row.get("dimensions")).isNull();
        assertThat(row.get("description")).isNull();
        assertThat(row.get("visibility")).isEqualTo("PUBLIC");
        assertThat(row.get("hidden_by_admin")).isEqualTo(false);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM artwork_images", Long.class)).isZero();
    }

    @Test
    void prihvacaDonjuGranicuITekucuGodinu() throws Exception {
        loginAs("ARTIST");

        send(body("Rani rad", 1900, "Ulje", null, null, "PRIVATE")).andExpect(status().isCreated());
        send(body("Novi rad", CURRENT_YEAR, "Akril", null, null, "PRIVATE")).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM artworks", Long.class)).isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"title", "year", "technique", "visibility"})
    void zahtijevaNaslovGodinuTehnikuIVidljivost(String field) throws Exception {
        loginAs("ARTIST");
        Map<String, Object> request = validRequest();
        request.remove(field);

        send(objectMapper.writeValueAsString(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM artworks", Long.class)).isZero();
    }

    @Test
    void odbijaNepoznatuVidljivost() throws Exception {
        loginAs("ARTIST");

        send(body("Morska tišina", 2024, "Ulje", null, null, "UNLISTED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM artworks", Long.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {1899, 0})
    void odbijaGodinuManjuOd1900(int year) throws Exception {
        loginAs("ARTIST");

        send(body("Morska tišina", year, "Ulje", null, null, "PRIVATE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("year"));
    }

    @Test
    void odbijaGodinuUBuducnosti() throws Exception {
        loginAs("ARTIST");

        send(body("Morska tišina", NEXT_YEAR, "Ulje", null, null, "PRIVATE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("year"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"title", "technique"})
    void odbijaNaslovITehnikuDužeOd200Znakova(String field) throws Exception {
        loginAs("ARTIST");
        String tooLong = "x".repeat(201);

        send(body("title".equals(field) ? tooLong : "Morska tišina", 2024,
                "technique".equals(field) ? tooLong : "Ulje", null, null, "PRIVATE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value(field));
    }

    @Test
    void odbijaDimenzijeDuzeOd100Znakova() throws Exception {
        loginAs("ARTIST");

        send(body("Morska tišina", 2024, "Ulje", "x".repeat(101), null, "PRIVATE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("dimensions"));
    }

    @Test
    void odbijaOpisDuziOd5000Znakova() throws Exception {
        loginAs("ARTIST");

        send(body("Morska tišina", 2024, "Ulje", null, "x".repeat(5001), "PRIVATE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("description"));
    }

    @Test
    void neprijavljenomVracaUnauthenticated() throws Exception {
        mockMvc.perform(post(PATH).with(anonymous()).with(withCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Morska tišina", 2024, "Ulje", null, null, "PRIVATE")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void organizatoruVracaForbidden() throws Exception {
        loginAs("ORGANIZER");

        send(body("Morska tišina", 2024, "Ulje", null, null, "PRIVATE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void bezCsrfTokenaVracaForbidden() throws Exception {
        loginAs("ARTIST");

        mockMvc.perform(post(PATH).contentType(MediaType.APPLICATION_JSON)
                        .content(body("Morska tišina", 2024, "Ulje", null, null, "PRIVATE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void korisnikuKojiJosNijeOdabraoUloguVracaOnboardingRequired() throws Exception {
        UUID artistId = loginAs("ARTIST");
        jdbc.update("UPDATE users SET status = 'ONBOARDING', role = NULL WHERE id = ?", artistId);

        send(body("Morska tišina", 2024, "Ulje", null, null, "PRIVATE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ONBOARDING_REQUIRED"));
    }

    @Test
    void blokiranomUmjetnikuVracaUserBlocked() throws Exception {
        UUID artistId = loginAs("ARTIST");
        jdbc.update("UPDATE users SET status = 'BLOCKED' WHERE id = ?", artistId);

        send(body("Morska tišina", 2024, "Ulje", null, null, "PRIVATE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("USER_BLOCKED"));
    }

    private ResultActions send(String body) throws Exception {
        return mockMvc.perform(post(PATH).with(withCsrf())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static Map<String, Object> validRequest() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("title", "Morska tišina");
        request.put("year", 2024);
        request.put("technique", "Ulje");
        request.put("dimensions", null);
        request.put("description", null);
        request.put("visibility", "PRIVATE");
        return request;
    }

    private static String body(String title, Integer year, String technique,
            String dimensions, String description, String visibility) {
        return "{\"title\":" + json(title) + ",\"year\":" + (year == null ? "null" : year)
                + ",\"technique\":" + json(technique) + ",\"dimensions\":" + json(dimensions)
                + ",\"description\":" + json(description) + ",\"visibility\":" + json(visibility) + "}";
    }

    private static String json(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
