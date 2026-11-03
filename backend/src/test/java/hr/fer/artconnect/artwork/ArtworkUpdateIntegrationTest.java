package hr.fer.artconnect.artwork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.support.IntegrationTestBase;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

class ArtworkUpdateIntegrationTest extends IntegrationTestBase {
    private static final String PATH = "/api/v1/artworks";

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void azuriraVlastitoDjeloIVracaPostojeceSlike() throws Exception {
        UUID artistId = loginAs("ARTIST");
        UUID artworkId = createArtwork(artistId);
        UUID imageId = addImage(artistId, artworkId);
        jdbc.update("UPDATE artworks SET hidden_by_admin = true WHERE id = ?", artworkId);
        OffsetDateTime oldUpdatedAt = jdbc.queryForObject(
                "SELECT updated_at FROM artworks WHERE id = ?", OffsetDateTime.class, artworkId);

        update(artworkId, validBody("Novi naslov", "PRIVATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(artworkId.toString()))
                .andExpect(jsonPath("$.artistId").value(artistId.toString()))
                .andExpect(jsonPath("$.title").value("Novi naslov"))
                .andExpect(jsonPath("$.year").value(2025))
                .andExpect(jsonPath("$.technique").value("Ulje"))
                .andExpect(jsonPath("$.dimensions").value("40 x 50 cm"))
                .andExpect(jsonPath("$.description").value("Novi opis"))
                .andExpect(jsonPath("$.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.hiddenByAdmin").value(true))
                .andExpect(jsonPath("$.images.length()").value(1))
                .andExpect(jsonPath("$.images[0].id").value(imageId.toString()));

        OffsetDateTime updatedAt = jdbc.queryForObject(
                "SELECT updated_at FROM artworks WHERE id = ?", OffsetDateTime.class, artworkId);
        assertThat(updatedAt).isAfter(oldUpdatedAt);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM artwork_images WHERE artwork_id = ?", Long.class, artworkId)).isEqualTo(1);
        mockMvc.perform(get(PATH + "/" + artworkId).with(anonymous()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void promjenaVidljivostiUPrivateUklanjaDjeloIzJavnogPopisa() throws Exception {
        UUID artistId = loginAs("ARTIST");
        UUID artworkId = createArtwork(artistId);
        String publicListPath = "/api/v1/artists/" + artistId + "/artworks";

        mockMvc.perform(get(publicListPath).with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(1));

        update(artworkId, validBody("Morska tišina", "PRIVATE")).andExpect(status().isOk());

        mockMvc.perform(get(publicListPath).with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta.total").value(0));
    }

    @Test
    void zahtijevaIspravnuAutentikacijuUloguIcsrf() throws Exception {
        UUID artistId = loginAs("ARTIST");
        UUID artworkId = createArtwork(artistId);

        mockMvc.perform(put(PATH + "/" + artworkId).with(anonymous()).with(withCsrf())
                        .contentType(MediaType.APPLICATION_JSON).content(validBody("Naslov", "PUBLIC")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));

        loginAs("ORGANIZER");
        update(artworkId, validBody("Naslov", "PUBLIC"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        loginAs("ARTIST");
        mockMvc.perform(put(PATH + "/" + artworkId)
                        .contentType(MediaType.APPLICATION_JSON).content(validBody("Naslov", "PUBLIC")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        UUID blockedArtistId = loginAs("ARTIST");
        UUID blockedArtworkId = createArtwork(blockedArtistId);
        jdbc.update("UPDATE users SET status = 'BLOCKED' WHERE id = ?", blockedArtistId);
        update(blockedArtworkId, validBody("Naslov", "PUBLIC"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("USER_BLOCKED"));
    }

    @Test
    void onboardingKorisnikuVracaOnboardingRequired() throws Exception {
        UUID artistId = loginAs("ARTIST");
        UUID artworkId = createArtwork(artistId);
        jdbc.update("UPDATE users SET status = 'ONBOARDING', role = NULL WHERE id = ?", artistId);

        update(artworkId, validBody("Naslov", "PUBLIC"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ONBOARDING_REQUIRED"));
    }

    @Test
    void neispravanZahtjevVlasnistvoINepostojeceDjeloVracaOdgovarajuceGreske() throws Exception {
        UUID ownerId = loginAs("ARTIST");
        UUID artworkId = createArtwork(ownerId);

        update(artworkId, "{\"year\":2025,\"technique\":\"Ulje\",\"visibility\":\"PUBLIC\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        loginAs("ARTIST");
        update(artworkId, validBody("Tuđe djelo", "PRIVATE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));

        update(UUID.randomUUID(), validBody("Nepostojeće djelo", "PRIVATE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    private UUID createArtwork(UUID artistId) throws Exception {
        String body = """
                {"title":"Morska tišina","year":2024,"technique":"Akril","dimensions":null,
                 "description":null,"visibility":"PUBLIC"}
                """;
        String response = mockMvc.perform(post(PATH).with(withCsrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID artworkId = UUID.fromString(objectMapper.readTree(response).get("id").asString());
        assertThat(jdbc.queryForObject(
                "SELECT artist_id FROM artworks WHERE id = ?", UUID.class, artworkId)).isEqualTo(artistId);
        return artworkId;
    }

    private UUID addImage(UUID artistId, UUID artworkId) {
        UUID fileId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO stored_files
                    (id, owner_id, purpose, original_name, content_type, size_bytes, storage_key)
                VALUES (?, ?, 'ARTWORK_IMAGE', 'artwork.jpg', 'image/jpeg', 1234, ?)
                """, fileId, artistId, fileId.toString());
        jdbc.update("""
                INSERT INTO artwork_images (id, artwork_id, file_id, position, alt_text)
                VALUES (?, ?, ?, 0, 'Test image')
                """, imageId, artworkId, fileId);
        return imageId;
    }

    private org.springframework.test.web.servlet.ResultActions update(UUID artworkId, String body) throws Exception {
        return mockMvc.perform(put(PATH + "/" + artworkId).with(withCsrf())
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String validBody(String title, String visibility) {
        return "{\"title\":\"" + title + "\",\"year\":2025,\"technique\":\"Ulje\","
                + "\"dimensions\":\"40 x 50 cm\",\"description\":\"Novi opis\","
                + "\"visibility\":\"" + visibility + "\"}";
    }
}
