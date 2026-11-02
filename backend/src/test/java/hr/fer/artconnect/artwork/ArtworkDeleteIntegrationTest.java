package hr.fer.artconnect.artwork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.file.service.FileStorageService;
import hr.fer.artconnect.support.IntegrationTestBase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class ArtworkDeleteIntegrationTest extends IntegrationTestBase {
    private static final String PATH = "/api/v1/artworks/";
    private static final byte[] JPEG = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00};

    @TempDir
    Path tempDir;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    FileStorageService fileStorage;

    @Test
    void briseDjeloSlikeMetapodatkeIDatotekeNakonCommita() throws Exception {
        UUID artistId = loginAs("ARTIST");
        UUID artworkId = createArtwork(artistId);
        List<Path> imagePaths = addImages(artistId, artworkId, 3);
        assertThat(imagePaths).allSatisfy(path -> assertThat(path).exists());

        mockMvc.perform(deleteRequest(artworkId).with(withCsrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM artworks WHERE id = ?", Long.class, artworkId)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM artwork_images WHERE artwork_id = ?", Long.class, artworkId)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM stored_files WHERE owner_id = ?", Long.class, artistId)).isZero();
        assertThat(imagePaths).allSatisfy(path -> assertThat(path).doesNotExist());
    }

    @Test
    void zahtijevaAutentikacijuUloguIcsrf() throws Exception {
        UUID artistId = loginAs("ARTIST");
        UUID artworkId = createArtwork(artistId);

        mockMvc.perform(deleteRequest(artworkId).with(anonymous()).with(withCsrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));

        loginAs("ORGANIZER");
        mockMvc.perform(deleteRequest(artworkId).with(withCsrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        loginAs("ARTIST");
        mockMvc.perform(deleteRequest(artworkId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void onboardingIblokiraniUmjetnikDobivajuForbidden() throws Exception {
        UUID artistId = loginAs("ARTIST");
        UUID artworkId = createArtwork(artistId);
        jdbc.update("UPDATE users SET status = 'ONBOARDING', role = NULL WHERE id = ?", artistId);

        mockMvc.perform(deleteRequest(artworkId).with(withCsrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ONBOARDING_REQUIRED"));

        UUID blockedArtistId = loginAs("ARTIST");
        UUID blockedArtworkId = createArtwork(blockedArtistId);
        jdbc.update("UPDATE users SET status = 'BLOCKED' WHERE id = ?", blockedArtistId);
        mockMvc.perform(deleteRequest(blockedArtworkId).with(withCsrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("USER_BLOCKED"));
    }

    @Test
    void tuđeINepostojeceDjeloVracajuNotFound() throws Exception {
        UUID ownerId = loginAs("ARTIST");
        UUID artworkId = createArtwork(ownerId);
        loginAs("ARTIST");

        mockMvc.perform(deleteRequest(artworkId).with(withCsrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        mockMvc.perform(deleteRequest(UUID.randomUUID()).with(withCsrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    private UUID createArtwork(UUID artistId) {
        UUID artworkId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO artworks (id, artist_id, title, year, technique, visibility)
                VALUES (?, ?, 'Test artwork', 2024, 'Oil', 'PUBLIC')
                """, artworkId, artistId);
        return artworkId;
    }

    private List<Path> addImages(UUID artistId, UUID artworkId, int count) throws Exception {
        Path root = tempDir.toAbsolutePath().normalize();
        ReflectionTestUtils.setField(fileStorage, "root", root);
        List<Path> paths = new ArrayList<>();
        for (int position = 0; position < count; position++) {
            UUID fileId = UUID.randomUUID();
            UUID imageId = UUID.randomUUID();
            String storageKey = "artwork_image/" + fileId + ".jpg";
            Path filePath = root.resolve(storageKey);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, JPEG);
            jdbc.update("""
                    INSERT INTO stored_files
                        (id, owner_id, purpose, original_name, content_type, size_bytes, storage_key)
                    VALUES (?, ?, 'ARTWORK_IMAGE', ?, 'image/jpeg', ?, ?)
                    """, fileId, artistId, "artwork-" + fileId + ".jpg", JPEG.length, storageKey);
            jdbc.update("""
                    INSERT INTO artwork_images (id, artwork_id, file_id, position, alt_text)
                    VALUES (?, ?, ?, ?, 'Test image')
                    """, imageId, artworkId, fileId, position);
            paths.add(filePath);
        }
        return paths;
    }

    private static MockHttpServletRequestBuilder deleteRequest(UUID artworkId) {
        return delete(PATH + artworkId);
    }
}
