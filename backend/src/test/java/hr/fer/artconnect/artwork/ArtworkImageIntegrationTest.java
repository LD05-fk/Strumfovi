package hr.fer.artconnect.artwork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

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

    @Autowired
    ObjectMapper objectMapper;

    private final List<Path> createdFilePaths = new ArrayList<>();

    @BeforeEach
    void resetUploadSettings() {
        jdbc.update("UPDATE system_settings SET value = '5242880' WHERE key = 'upload.artwork.maxImageBytes'");
        jdbc.update("UPDATE system_settings SET value = '3' WHERE key = 'upload.artwork.maxImagesPerArtwork'");
    }

    @AfterEach
    void cleanCreatedUploadFiles() throws IOException {
        for (Path path : createdFilePaths) {
            Files.deleteIfExists(path);
        }
        createdFilePaths.clear();
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

    @Test
    void briseSlikuMetapodatkeIDatotekuIAzuriraDjelo() throws Exception {
        UUID artistId = loginAs("ARTIST");
        UUID artworkId = createArtwork(artistId);
        var uploaded = upload(artworkId, artistId, "small.jpg", read("small.jpg"), null)
                .andExpect(status().isCreated());
        UUID imageId = imageId(uploaded.andReturn().getResponse().getContentAsString());
        UUID fileId = fileId(uploaded.andReturn().getResponse().getContentAsString());
        String storageKey = jdbc.queryForObject(
                "SELECT storage_key FROM stored_files WHERE id = ?", String.class, fileId);
        Path storedPath = Path.of("target/test-uploads").toAbsolutePath().normalize().resolve(storageKey);
        assertThat(storedPath).exists();

        jdbc.update("UPDATE artworks SET updated_at = '2000-01-01T00:00:00Z' WHERE id = ?", artworkId);
        var deleted = deleteAs(artworkId, imageId, artistId)
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(deleted.getResponse().getContentAsString()).isEmpty();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM artwork_images WHERE id = ?", Long.class, imageId))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stored_files WHERE id = ?", Long.class, fileId)).isZero();
        assertThat(storedPath).doesNotExist();
        assertThat(jdbc.queryForObject("SELECT updated_at FROM artworks WHERE id = ?", OffsetDateTime.class, artworkId))
                .isAfter(OffsetDateTime.parse("2000-01-01T00:00:00Z"));
    }

    @Test
    void brisanjeOslobadaLimitINeMijenjaPozicijePreostalihSlika() throws Exception {
        UUID artistId = loginAs("ARTIST");
        UUID artworkId = createArtwork(artistId);
        UUID firstImageId = imageId(upload(artworkId, artistId, "small.jpg", read("small.jpg"), null)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        UUID secondImageId = imageId(upload(artworkId, artistId, "small.jpg", read("small.jpg"), null)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        upload(artworkId, artistId, "small.jpg", read("small.jpg"), null).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM artwork_images WHERE artwork_id = ?",
                Long.class, artworkId)).isEqualTo(3);

        deleteAs(artworkId, secondImageId, artistId).andExpect(status().isNoContent());
        assertThat(jdbc.queryForList(
                "SELECT position FROM artwork_images WHERE artwork_id = ? ORDER BY position", Short.class, artworkId))
                .containsExactly((short) 0, (short) 2);

        var replacement = upload(artworkId, artistId, "small.png", read("small.png"), null)
                .andExpect(status().isCreated()).andReturn();
        assertThat(jdbc.queryForObject(
                "SELECT position FROM artwork_images WHERE id = ?", Short.class, firstImageId)).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT position FROM artwork_images
                WHERE artwork_id = ? AND id <> ? AND id <> ?
                ORDER BY position DESC LIMIT 1
                """, Short.class, artworkId, firstImageId, secondImageId)).isEqualTo((short) 3);
        assertThat(imageId(replacement.getResponse().getContentAsString())).isNotNull();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM artwork_images WHERE artwork_id = ?",
                Long.class, artworkId)).isEqualTo(3);
    }

    @Test
    void brisanjeProvjeravaAutentikacijuUloguVlasnistvoIPovezanostSlike() throws Exception {
        UUID artistId = loginAs("ARTIST");
        UUID artworkId = createArtwork(artistId);
        UUID imageId = imageId(upload(artworkId, artistId, "small.jpg", read("small.jpg"), null)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());

        mockMvc.perform(delete(deletePath(artworkId, imageId)).with(anonymous()).with(withCsrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));

        mockMvc.perform(delete(deletePath(artworkId, imageId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        loginAs("ORGANIZER");
        mockMvc.perform(delete(deletePath(artworkId, imageId)).with(withCsrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        UUID foreignArtistId = loginAs("ARTIST");
        UUID foreignArtworkId = createArtwork(foreignArtistId);
        TestSecurityContextHolder.setAuthentication(TestPrincipals.authentication(users.findById(artistId).orElseThrow()));
        deleteAs(foreignArtworkId, imageId, artistId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        deleteAs(artworkId, UUID.randomUUID(), artistId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));

        UUID otherArtworkId = createArtwork(artistId);
        UUID otherImageId = imageId(upload(otherArtworkId, artistId, "small.png", read("small.png"), null)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        deleteAs(artworkId, otherImageId, artistId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM artwork_images WHERE id = ?", Long.class, otherImageId)).isEqualTo(1);

        jdbc.update("UPDATE users SET status = 'ONBOARDING', role = NULL WHERE id = ?", artistId);
        deleteAs(artworkId, imageId, artistId)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ONBOARDING_REQUIRED"));
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
        var result = mockMvc.perform(request);
        if (result.andReturn().getResponse().getStatus() == 201) {
            UUID fileId = fileId(result.andReturn().getResponse().getContentAsString());
            String storageKey = jdbc.queryForObject(
                    "SELECT storage_key FROM stored_files WHERE id = ?", String.class, fileId);
            createdFilePaths.add(Path.of("target/test-uploads").toAbsolutePath().normalize().resolve(storageKey));
        }
        return result;
    }

    private ResultActions deleteAs(UUID artworkId, UUID imageId, UUID artistId) throws Exception {
        TestSecurityContextHolder.setAuthentication(TestPrincipals.authentication(users.findById(artistId).orElseThrow()));
        return mockMvc.perform(delete(deletePath(artworkId, imageId)).with(withCsrf()));
    }

    private UUID imageId(String response) throws Exception {
        return UUID.fromString(objectMapper.readTree(response).get("id").asString());
    }

    private UUID fileId(String response) throws Exception {
        return UUID.fromString(objectMapper.readTree(response).get("file").get("id").asString());
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

    private static String deletePath(UUID artworkId, UUID imageId) {
        return imagePath(artworkId) + "/" + imageId;
    }

    private static byte[] read(String name) throws IOException {
        return Files.readAllBytes(FILES.resolve(name));
    }
}
