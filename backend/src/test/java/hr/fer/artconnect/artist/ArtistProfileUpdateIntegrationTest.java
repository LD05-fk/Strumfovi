package hr.fer.artconnect.artist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.support.IntegrationTestBase;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

class ArtistProfileUpdateIntegrationTest extends IntegrationTestBase {
    private static final String PATH = "/api/v1/me/artist-profile";
    private static final UUID INACTIVE_FIELD_ID = UUID.randomUUID();
    private static final String INACTIVE_FIELD_CODE = "put-inactive-" + INACTIVE_FIELD_ID.toString().substring(0, 8);

    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void removeInactiveTestArtField() {
        jdbc.update("DELETE FROM artist_profile_art_fields WHERE art_field_id = ?", INACTIVE_FIELD_ID);
        jdbc.update("DELETE FROM art_fields WHERE id = ?", INACTIVE_FIELD_ID);
    }

    @Test
    void stvaraProfilIZatimPotpunoZamjenjujePodrucjaIPoveznice() throws Exception {
        UUID artistId = loginAs("ARTIST");
        UUID paintingId = artFieldId("slikarstvo");
        UUID photoId = artFieldId("fotografija");

        send("""
                {"biography":"  Biografija  ","artistStatement":"  Izjava  ",
                 "artFieldIds":["%s"],"links":[
                   {"label":"  Instagram  ","url":" https://example.test/ana "},
                   {"label":"   ","url":"http://example.test/gallery"}
                 ]}
                """.formatted(paintingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(artistId.toString()))
                .andExpect(jsonPath("$.biography").value("Biografija"))
                .andExpect(jsonPath("$.artistStatement").value("Izjava"))
                .andExpect(jsonPath("$.artFields.length()").value(1))
                .andExpect(jsonPath("$.artFields[0].id").value(paintingId.toString()))
                .andExpect(jsonPath("$.links.length()").value(2))
                .andExpect(jsonPath("$.links[0].label").value("Instagram"))
                .andExpect(jsonPath("$.links[0].url").value("https://example.test/ana"))
                .andExpect(jsonPath("$.links[1].url").value("http://example.test/gallery"))
                .andExpect(jsonPath("$.links[1].label").value(org.hamcrest.Matchers.nullValue()));

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM artist_profiles WHERE user_id = ?", Long.class, artistId)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT updated_at FROM artist_profiles WHERE user_id = ?", Object.class, artistId)).isNotNull();
        assertThat(jdbc.queryForObject(
                "SELECT position FROM artist_links WHERE user_id = ? AND url = ?",
                Integer.class, artistId, "https://example.test/ana")).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT position = 1 FROM artist_links WHERE user_id = ? AND url = ?",
                Boolean.class, artistId, "http://example.test/gallery")).isTrue();
        assertThat(jdbc.queryForObject(
                "SELECT label FROM artist_links WHERE user_id = ? AND url = ?",
                String.class, artistId, "http://example.test/gallery")).isNull();
        jdbc.update("UPDATE artist_profiles SET updated_at = '2000-01-01T00:00:00Z' WHERE user_id = ?", artistId);

        send("""
                {"biography":null,"artistStatement":null,"artFieldIds":["%s"],"links":[]}
                """.formatted(photoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biography").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.artistStatement").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.artFields.length()").value(1))
                .andExpect(jsonPath("$.artFields[0].id").value(photoId.toString()))
                .andExpect(jsonPath("$.links").isEmpty());

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM artist_profiles WHERE user_id = ?", Long.class, artistId)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM artist_profile_art_fields WHERE user_id = ?", Long.class, artistId)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM artist_links WHERE user_id = ?", Long.class, artistId)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT updated_at FROM artist_profiles WHERE user_id = ?", OffsetDateTime.class, artistId))
                .isAfter(OffsetDateTime.parse("2000-01-01T00:00:00Z"));
    }

    @Test
    void zadrzavaNeaktivnoPodrucjeKojeJeVecVezanoAliOdbijaNovo() throws Exception {
        UUID artistId = loginAs("ARTIST");
        insertInactiveField();
        jdbc.update("INSERT INTO artist_profiles (user_id) VALUES (?)", artistId);
        jdbc.update("INSERT INTO artist_profile_art_fields (user_id, art_field_id) VALUES (?, ?)",
                artistId, INACTIVE_FIELD_ID);

        send("""
                {"biography":null,"artistStatement":null,"artFieldIds":["%s"],"links":[]}
                """.formatted(INACTIVE_FIELD_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artFields[0].id").value(INACTIVE_FIELD_ID.toString()));

        UUID anotherArtistId = loginAs("ARTIST");
        send("""
                {"biography":null,"artistStatement":null,"artFieldIds":["%s"],"links":[]}
                """.formatted(INACTIVE_FIELD_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details[0].field").value("artFieldIds"));
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM artist_profiles WHERE user_id = ?", Long.class, anotherArtistId)).isZero();
    }

    @Test
    void vracaValidationErrorZaOgranicenjaIUrl() throws Exception {
        loginAs("ARTIST");
        UUID knownId = artFieldId("slikarstvo");

        send(body("x".repeat(5001), List.of(), List.of()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("biography"));

        send("{\"biography\":null,\"artistStatement\":\"" + "x".repeat(5001)
                        + "\",\"artFieldIds\":[],\"links\":[]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details[0].field").value("artistStatement"));

        send(body(null, List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()), List.of()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details[0].field").value("artFieldIds"));

        send(body(null, List.of(knownId, knownId), List.of()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details[0].field").value("artFieldIds"));

        send(body(null, List.of(UUID.randomUUID()), List.of()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details[0].field").value("artFieldIds"));

        send(body(null, List.of(), java.util.Collections.nCopies(11,
                        "{\"label\":null,\"url\":\"https://example.test\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details[0].field").value("links"));

        send(body(null, List.of(), List.of("{\"label\":null,\"url\":\"ftp://example.test\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details[0].field").value("links[0].url"));

        send(body(null, List.of(), List.of("{\"label\":null,\"url\":\"HTTPS://example.test\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details[0].field").value("links[0].url"));

        send(body(null, List.of(), List.of("{\"label\":null,\"url\":\"https://\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details[0].field").value("links[0].url"));

        send(body(null, List.of(), List.of("{\"label\":\"" + "x".repeat(61)
                        + "\",\"url\":\"https://example.test\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details[0].field").value("links[0].label"));

        send(body(null, List.of(), List.of("{\"label\":null,\"url\":\"https://"
                        + "x".repeat(1025) + ".test\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details[0].field").value("links[0].url"));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM artist_profiles", Long.class)).isZero();
    }

    @Test
    void prijavaUlogaICsrfSuObavezni() throws Exception {
        mockMvc.perform(put(PATH).with(anonymous()).with(withCsrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(null, List.of(), List.of())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));

        loginAs("ORGANIZER");
        send(body(null, List.of(), List.of()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        loginAs("ARTIST");
        mockMvc.perform(put(PATH).contentType(MediaType.APPLICATION_JSON)
                        .content(body(null, List.of(), List.of())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        UUID onboardingArtistId = loginAs("ARTIST");
        jdbc.update("UPDATE users SET status = 'ONBOARDING', role = NULL WHERE id = ?", onboardingArtistId);
        send(body(null, List.of(), List.of()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ONBOARDING_REQUIRED"));

        UUID blockedArtistId = loginAs("ARTIST");
        jdbc.update("UPDATE users SET status = 'BLOCKED' WHERE id = ?", blockedArtistId);
        send(body(null, List.of(), List.of()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("USER_BLOCKED"));
    }

    private ResultActions send(String body) throws Exception {
        return mockMvc.perform(put(PATH).with(withCsrf())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private UUID artFieldId(String code) {
        return jdbc.queryForObject("SELECT id FROM art_fields WHERE code = ?", UUID.class, code);
    }

    private void insertInactiveField() {
        jdbc.update("""
                INSERT INTO art_fields (id, code, name, active, position)
                VALUES (?, ?, 'Neaktivno testno područje', false, 99)
                """, INACTIVE_FIELD_ID, INACTIVE_FIELD_CODE);
    }

    private static String body(String biography, List<UUID> artFieldIds, List<String> links) {
        String ids = artFieldIds.stream().map(id -> "\"" + id + "\"").collect(java.util.stream.Collectors.joining(","));
        return "{\"biography\":" + json(biography) + ",\"artistStatement\":null,\"artFieldIds\":["
                + ids + "],\"links\":[" + String.join(",", links) + "]}";
    }

    private static String json(String value) {
        return value == null ? "null" : "\"" + value + "\"";
    }
}
