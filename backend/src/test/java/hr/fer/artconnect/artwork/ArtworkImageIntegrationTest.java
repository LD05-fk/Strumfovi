package hr.fer.artconnect.artwork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.artwork.domain.Artwork;
import hr.fer.artconnect.artwork.domain.ArtworkRepository;
import hr.fer.artconnect.artwork.domain.ArtworkVisibility;
import hr.fer.artconnect.auth.domain.UserRepository;
import hr.fer.artconnect.support.IntegrationTestBase;
import hr.fer.artconnect.support.TestPrincipals;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(properties = "artconnect.storage.root=target/test-uploads")
class ArtworkImageIntegrationTest extends IntegrationTestBase {
    private static final String ARTWORKS_PATH = "/api/v1/artworks";
    private static final int DEFAULT_MAX_IMAGE_BYTES = 5 * 1024 * 1024;
    private static final Path FILES = Path.of("src/test/resources/files");
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a
    };

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ArtworkRepository artworks;

    @Autowired
    UserRepository users;

    @BeforeEach
    void resetUploadSettings() {
        jdbc.update("UPDATE system_settings SET value = '5242880' WHERE key = 'upload.artwork.maxImageBytes'");
        jdbc.update("UPDATE system_settings SET value = '3' WHERE key = 'upload.artwork.maxImagesPerArtwork'");
    }

    @Test
    void ucitavaJpgIPngSaRednimPozicijamaIZadanimAltTextom() throws Exception {
        UUID artistId = loginAs("ARTIST");
        UUID artworkId = createArtwork(artistId);
        jdbc.update("UPDATE artworks SET updated_at = '2000-01-01T00:00:00Z' WHERE id = ?", artworkId);

        upload(artworkId, artistId, "small.jpg", read("small.jpg"), null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.file.originalName").value("small.jpg"))
                .andExpect(jsonPath("$.file.contentType").value("image/jpeg"))
                .andExpect(jsonPath("$.file.sizeBytes").isNumber())
                .andExpect(jsonPath("$.file.url").exists())
                .andExpect(jsonPath("$.altText").value("Test artwork"));
        upload(artworkId, artistId, "small.png", read("small.png"), "  Opis slike  ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.file.contentType").value("image/png"))
                .andExpect(jsonPath("$.altText").value("Opis slike"));

        assertThat(jdbc.queryForList(
                "SELECT position FROM artwork_images WHERE artwork_id = ? ORDER BY position", Short.class, artworkId))
                .containsExactly((short) 0, (short) 1);
        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM stored_files
                WHERE owner_id = ? AND purpose = 'ARTWORK_IMAGE'
                """, Long.class, artistId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT updated_at FROM artworks WHERE id = ?", OffsetDateTime.class, artworkId))
                .isAfter(OffsetDateTime.parse("2000-01-01T00:00:00Z"));
    }

    @Test
    void vracaDokumentiraneValidacijskeITipskeGreske() throws Exception {
        UUID artistId = loginAs("ARTIST");
        UUID artworkId = createArtwork(artistId);

        mockMvc.perform(multipart(imagePath(artworkId)).with(withCsrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        upload(artworkId, artistId, "empty.jpg", new byte[0], null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        upload(artworkId, artistId, "small.jpg", read("small.jpg"), "x".repeat(301))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        upload(artworkId, artistId, "small.pdf", read("small.pdf"), null)
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_FILE_TYPE"));
        upload(artworkId, artistId, "renamed.jpg", read("not-an-image.txt"), null)
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_FILE_TYPE"));

        byte[] tooLarge = new byte[DEFAULT_MAX_IMAGE_BYTES + 1];
        System.arraycopy(PNG_SIGNATURE, 0, tooLarge, 0, PNG_SIGNATURE.length);
        upload(artworkId, artistId, "large.png", tooLarge, null)
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.error.code").value("FILE_TOO_LARGE"));
    }

    @Test
    void vracaSigurnosneIPostojanostneGreske() throws Exception {
        UUID artistId = loginAs("ARTIST");
        UUID artworkId = createArtwork(artistId);
        byte[] jpeg = read("small.jpg");

        mockMvc.perform(multipart(imagePath(artworkId)).file(part("small.jpg", jpeg))
                        .with(anonymous()).with(withCsrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        mockMvc.perform(multipart(imagePath(artworkId)).file(part("small.jpg", jpeg)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        UUID organizerId = loginAs("ORGANIZER");
        UUID organizerArtworkId = createArtwork(organizerId);
        upload(organizerArtworkId, organizerId, "small.jpg", jpeg, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        UUID foreignArtistId = loginAs("ARTIST");
        UUID foreignArtworkId = createArtwork(foreignArtistId);
        TestSecurityContextHolder.setAuthentication(TestPrincipals.authentication(users.findById(artistId).orElseThrow()));
        upload(foreignArtworkId, artistId, "small.jpg", jpeg, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        upload(UUID.randomUUID(), artistId, "small.jpg", jpeg, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));

        jdbc.update("UPDATE users SET status = 'ONBOARDING', role = NULL WHERE id = ?", artistId);
        upload(artworkId, artistId, "small.jpg", jpeg, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ONBOARDING_REQUIRED"));
    }

    @Test
    void citaPromijenjeniLimitIzSystemSettings() throws Exception {
        UUID artistId = loginAs("ARTIST");
        UUID artworkId = createArtwork(artistId);
        jdbc.update("UPDATE system_settings SET value = '1' WHERE key = 'upload.artwork.maxImagesPerArtwork'");

        upload(artworkId, artistId, "small.jpg", read("small.jpg"), null).andExpect(status().isCreated());
        upload(artworkId, artistId, "small.png", read("small.png"), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("IMAGE_LIMIT_REACHED"));
    }

    @Test
    void paralelniUploadiNeMoguPrijeciLimitBrojaSlika() throws Exception {
        UUID artistId = loginAs("ARTIST");
        UUID artworkId = createArtwork(artistId);
        byte[] jpeg = read("small.jpg");
        upload(artworkId, artistId, "small.jpg", jpeg, null).andExpect(status().isCreated());
        upload(artworkId, artistId, "small.jpg", jpeg, null).andExpect(status().isCreated());

        var artistAuth = authentication(TestPrincipals.authentication(users.findById(artistId).orElseThrow()));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> concurrentUpload(artworkId, jpeg, artistAuth, ready, start));
            var second = executor.submit(() -> concurrentUpload(artworkId, jpeg, artistAuth, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(java.util.List.of(first.get(), second.get())).containsExactlyInAnyOrder(201, 409);
        }
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM artwork_images WHERE artwork_id = ?", Long.class, artworkId)).isEqualTo(3);
    }

    private int concurrentUpload(UUID artworkId, byte[] jpeg, RequestPostProcessor auth,
            CountDownLatch ready, CountDownLatch start) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent upload test did not start.");
        }
        return mockMvc.perform(multipart(imagePath(artworkId))
                        .file(part("small.jpg", jpeg)).with(auth).with(withCsrf()))
                .andReturn().getResponse().getStatus();
    }

    private ResultActions upload(UUID artworkId, UUID artistId, String name, byte[] bytes, String altText)
            throws Exception {
        TestSecurityContextHolder.setAuthentication(TestPrincipals.authentication(users.findById(artistId).orElseThrow()));
        var request = multipart(imagePath(artworkId)).file(part(name, bytes)).with(withCsrf());
        if (altText != null) {
            request.param("altText", altText);
        }
        return mockMvc.perform(request);
    }

    private UUID createArtwork(UUID artistId) {
        var artist = users.findById(artistId).orElseThrow();
        Artwork saved = artworks.save(Artwork.create(artist, "Test artwork", (short) 2024,
                "Oil", null, null, ArtworkVisibility.PUBLIC));
        return saved.getId();
    }

    private static MockMultipartFile part(String name, byte[] bytes) {
        return new MockMultipartFile("file", name, MediaType.APPLICATION_OCTET_STREAM_VALUE, bytes);
    }

    private static String imagePath(UUID artworkId) {
        return ARTWORKS_PATH + "/" + artworkId + "/images";
    }

    private static byte[] read(String name) throws IOException {
        return Files.readAllBytes(FILES.resolve(name));
    }
}
