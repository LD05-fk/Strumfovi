package hr.fer.artconnect.portfolio.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.support.IntegrationTestBase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.ObjectMapper;

/** PORT-03 i PORT-04: djela i slike kroz stvarne sesije, sigurnosne filtre i PostgreSQL. */
@ActiveProfiles("dev")
// Nakon brisanja privremene mape drugi testovi ne smiju preuzeti ovaj Springov kontekst.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ArtworkLifecycleIT extends IntegrationTestBase {
    private static final String PATH = "/api/v1/artworks";
    private static final String PUBLIC_BODY = """
            {"title":"Gradski motivi","year":2024,"technique":"Akril",
             "dimensions":null,"description":null,"visibility":"PUBLIC"}
            """;

    // Mapa vrijedi za ovu klasu; JUnit je nakon izvođenja automatski uklanja.
    @TempDir
    static Path storageRoot;

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("artconnect.storage.root", () -> storageRoot.toString());
    }

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper objectMapper;

    private PortfolioFixtures fixtures;
    private String originalMaxBytes;
    private String originalMaxImages;

    @BeforeEach
    void prepareFixtures() {
        fixtures = new PortfolioFixtures(jdbc, mockMvc);
        // Ovi scenariji ispituju zadane granice, ne postavke prethodnog testa.
        originalMaxBytes = jdbc.queryForObject(
                "SELECT value FROM system_settings WHERE key = 'upload.artwork.maxImageBytes'", String.class);
        originalMaxImages = jdbc.queryForObject(
                "SELECT value FROM system_settings WHERE key = 'upload.artwork.maxImagesPerArtwork'", String.class);
        jdbc.update("UPDATE system_settings SET value = '5242880' WHERE key = 'upload.artwork.maxImageBytes'");
        jdbc.update("UPDATE system_settings SET value = '3' WHERE key = 'upload.artwork.maxImagesPerArtwork'");
    }

    @AfterEach
    void removeFixtures() throws Exception {
        // Izvodi se i nakon pada testa: uklanja njegove korisnike, djela i sesije.
        try {
            fixtures.cleanup();
        } finally {
            jdbc.update("UPDATE system_settings SET value = ? WHERE key = 'upload.artwork.maxImageBytes'", originalMaxBytes);
            jdbc.update("UPDATE system_settings SET value = ? WHERE key = 'upload.artwork.maxImagesPerArtwork'", originalMaxImages);
            // Baza ne briše datoteke s diska. Čistimo ih i kad test prekine prije DELETE-a.
            try (var paths = Files.walk(storageRoot)) {
                for (Path path : paths.filter(Files::isRegularFile).toList()) {
                    Files.delete(path);
                }
            }
        }
    }

    @Test
    void umjetnikStvaraDjeloMijenjaVidljivostIBriseGa() throws Exception {
        UUID artistId = fixtures.user("ARTIST", "ACTIVE");
        var user = fixtures.login(artistId);

        // Sesija i CSRF token dobiveni su preko istih endpointa koje koristi frontend.
        String response = mockMvc.perform(post(PATH)
                        .cookie(user.session(), user.csrf())
                        .header("X-XSRF-TOKEN", user.csrf().getValue())
                        .contentType(MediaType.APPLICATION_JSON).content(PUBLIC_BODY))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(response).get("id").asText();

        // Bez kolačića čitamo kao posjetitelj: novo javno djelo mora biti na popisu.
        mockMvc.perform(get("/api/v1/artists/{id}/artworks", artistId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(id))
                .andExpect(jsonPath("$.meta.total").value(1));

        mockMvc.perform(put(PATH + "/{id}", id)
                        .cookie(user.session(), user.csrf())
                        .header("X-XSRF-TOKEN", user.csrf().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PUBLIC_BODY.replace("PUBLIC", "PRIVATE")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/artists/{id}/artworks", artistId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta.total").value(0));

        // Privatno djelo ostaje dostupno vlasniku sve dok ga on ne obriše.
        mockMvc.perform(get(PATH + "/{id}", id).cookie(user.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("PRIVATE"));
        mockMvc.perform(delete(PATH + "/{id}", id)
                        .cookie(user.session(), user.csrf())
                        .header("X-XSRF-TOKEN", user.csrf().getValue()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/artists/{id}/artworks", artistId).cookie(user.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void triSlikeDosezuLimitBrisanjeGaOslobadaABrisanjeDjelaCistiSveDatoteke() throws Exception {
        UUID artistId = fixtures.user("ARTIST", "ACTIVE");
        UUID artworkId = fixtures.artwork(artistId, OffsetDateTime.now());
        var user = fixtures.login(artistId);
        byte[] jpeg = Files.readAllBytes(Path.of("src/test/resources/files/small.jpg"));
        var images = new ArrayList<tools.jackson.databind.JsonNode>();

        // Tri slike prolaze kroz stvarni servis i spremaju se u bazu i @TempDir.
        for (int i = 0; i < 3; i++) {
            String response = fixtures.uploadImage(artworkId, user, "art.jpg", jpeg)
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            images.add(objectMapper.readTree(response));
        }
        fixtures.uploadImage(artworkId, user, "cetvrta.jpg", jpeg)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("IMAGE_LIMIT_REACHED"));

        var first = images.removeFirst();
        UUID firstFileId = UUID.fromString(first.get("file").get("id").asText());
        Path firstPath = storageRoot.resolve(jdbc.queryForObject(
                "SELECT storage_key FROM stored_files WHERE id = ?", String.class, firstFileId));
        assertThat(firstPath).exists();
        mockMvc.perform(delete(PATH + "/{id}/images/{imageId}", artworkId, first.get("id").asText())
                        .cookie(user.session(), user.csrf())
                        .header("X-XSRF-TOKEN", user.csrf().getValue()))
                .andExpect(status().isNoContent());
        assertThat(firstPath).doesNotExist();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stored_files WHERE id = ?", Long.class, firstFileId)).isZero();
        mockMvc.perform(get(first.get("file").get("url").asText())).andExpect(status().isNotFound());

        // Nakon brisanja ponovno ima mjesta. Na kraju brišemo djelo s točno tri slike.
        String replacement = fixtures.uploadImage(artworkId, user, "zamjena.jpg", jpeg)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        images.add(objectMapper.readTree(replacement));
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM artwork_images WHERE artwork_id = ?", Long.class, artworkId)).isEqualTo(3);
        List<Path> storedPaths = jdbc.queryForList(
                "SELECT storage_key FROM stored_files WHERE owner_id = ?", String.class, artistId)
                .stream().map(storageRoot::resolve).toList();
        assertThat(storedPaths).hasSize(3).allSatisfy(path -> assertThat(path).exists());

        mockMvc.perform(delete(PATH + "/{id}", artworkId)
                        .cookie(user.session(), user.csrf())
                        .header("X-XSRF-TOKEN", user.csrf().getValue()))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM artworks WHERE id = ?", Long.class, artworkId)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM artwork_images WHERE artwork_id = ?", Long.class, artworkId)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stored_files WHERE owner_id = ?", Long.class, artistId)).isZero();
        assertThat(storedPaths).allSatisfy(path -> assertThat(path).doesNotExist());
        for (var image : images) {
            mockMvc.perform(get(image.get("file").get("url").asText())).andExpect(status().isNotFound());
        }
        try (var paths = Files.walk(storageRoot)) {
            assertThat(paths.filter(Files::isRegularFile).toList()).isEmpty();
        }
    }

    @Test
    void odbijaPdfLazniPngIJpgJedanBajtIznadPetMiBBezOstataka() throws Exception {
        UUID artistId = fixtures.user("ARTIST", "ACTIVE");
        UUID artworkId = fixtures.artwork(artistId, OffsetDateTime.now());
        var user = fixtures.login(artistId);

        fixtures.uploadImage(artworkId, user, "document.pdf",
                        Files.readAllBytes(Path.of("src/test/resources/files/small.pdf")))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_FILE_TYPE"));
        // Nastavak .png ne smije pretvoriti tekst u dopuštenu sliku.
        fixtures.uploadImage(artworkId, user, "tekst.png", "Ovo nije slika".getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.code").value("UNSUPPORTED_FILE_TYPE"));
        byte[] largeJpeg = java.util.Arrays.copyOf(
                Files.readAllBytes(Path.of("src/test/resources/files/small.jpg")), 5 * 1024 * 1024 + 1);
        fixtures.uploadImage(artworkId, user, "big.jpg", largeJpeg)
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.error.code").value("FILE_TOO_LARGE"));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM artwork_images WHERE artwork_id = ?", Long.class, artworkId)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM stored_files WHERE owner_id = ?", Long.class, artistId)).isZero();
        try (var paths = Files.walk(storageRoot)) {
            assertThat(paths.filter(Files::isRegularFile).toList()).isEmpty();
        }
    }

    @ParameterizedTest
    @CsvSource({"ARTIST, ACTIVE, 404, NOT_FOUND", "ORGANIZER, ACTIVE, 403, FORBIDDEN",
            ", ONBOARDING, 403, ONBOARDING_REQUIRED"})
    void slikeNeMozeDodavatiNiBrisatiKorisnikBezPrava(String role, String userStatus, int statusCode, String errorCode)
            throws Exception {
        UUID ownerId = fixtures.user("ARTIST", "ACTIVE");
        UUID artworkId = fixtures.artwork(ownerId, OffsetDateTime.now());
        var owner = fixtures.login(ownerId);
        byte[] jpeg = Files.readAllBytes(Path.of("src/test/resources/files/small.jpg"));
        String response = fixtures.uploadImage(artworkId, owner, "art.jpg", jpeg)
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String imageId = objectMapper.readTree(response).get("id").asText();
        var other = fixtures.login(fixtures.user(role, userStatus));

        // Ispravni kolačići i token dokazuju da je razlog odbijanja pravo pristupa.
        fixtures.uploadImage(artworkId, other, "art.jpg", jpeg)
                .andExpect(status().is(statusCode))
                .andExpect(jsonPath("$.error.code").value(errorCode));
        mockMvc.perform(delete(PATH + "/{id}/images/{imageId}", artworkId, imageId)
                        .cookie(other.session(), other.csrf())
                        .header("X-XSRF-TOKEN", other.csrf().getValue()))
                .andExpect(status().is(statusCode))
                .andExpect(jsonPath("$.error.code").value(errorCode));
        mockMvc.perform(get(PATH + "/{id}", artworkId).cookie(owner.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.images.length()").value(1))
                .andExpect(jsonPath("$.images[0].id").value(imageId));
    }

    @Test
    void drugaStranicaVracaDesetOdDvadesetPetDjelaOdNajnovijeg() throws Exception {
        UUID artistId = fixtures.user("ARTIST", "ACTIVE");
        OffsetDateTime start = OffsetDateTime.parse("2024-01-01T12:00:00+01:00");
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            ids.add(fixtures.artwork(artistId, start.plusDays(i)).toString());
        }

        // Djela smo dodali od najstarijeg. Druga stranica treba položaje 10–19
        // obrnutog popisa; time provjeravamo i granice stranice i redoslijed.
        Collections.reverse(ids);
        String response = mockMvc.perform(get("/api/v1/artists/{id}/artworks", artistId)
                        .param("page", "2").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(10))
                .andExpect(jsonPath("$.meta.page").value(2))
                .andExpect(jsonPath("$.meta.pageSize").value(10))
                .andExpect(jsonPath("$.meta.total").value(25))
                .andReturn().getResponse().getContentAsString();
        List<String> actualIds = new ArrayList<>();
        for (var artwork : objectMapper.readTree(response).get("data")) {
            actualIds.add(artwork.get("id").asText());
        }
        assertThat(actualIds).containsExactlyElementsOf(ids.subList(10, 20));
    }

    @Test
    void drugiUmjetnikNeMozeUreditiNiObrisatiTudeDjelo() throws Exception {
        UUID ownerId = fixtures.user("ARTIST", "ACTIVE");
        UUID artworkId = fixtures.artwork(ownerId, OffsetDateTime.now());
        var other = fixtures.login(fixtures.user("ARTIST", "ACTIVE"));

        // Valjana prijava i CSRF isključuju pogrešku prijave kao razlog odbijanja.
        for (var request : List.of(put(PATH + "/{id}", artworkId), delete(PATH + "/{id}", artworkId))) {
            mockMvc.perform(request
                            .cookie(other.session(), other.csrf())
                            .header("X-XSRF-TOKEN", other.csrf().getValue())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(PUBLIC_BODY.replace("PUBLIC", "PRIVATE")))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }

        // Odbijene radnje ne smiju ni promijeniti vidljivost ni ukloniti djelo.
        mockMvc.perform(get(PATH + "/{id}", artworkId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visibility").value("PUBLIC"));
    }

    @ParameterizedTest
    @CsvSource({"ORGANIZER, ACTIVE, FORBIDDEN", ", ONBOARDING, ONBOARDING_REQUIRED"})
    void odbijaDodavanjeUredivanjeIBrisanjeBezPrava(String role, String userStatus, String errorCode)
            throws Exception {
        UUID ownerId = fixtures.user("ARTIST", "ACTIVE");
        UUID artworkId = fixtures.artwork(ownerId, OffsetDateTime.now());
        var user = fixtures.login(fixtures.user(role, userStatus));

        // Isti scenarij vrijedi za dvije vrste korisnika, ali očekivani kod je različit.
        for (var request : List.of(post(PATH), put(PATH + "/{id}", artworkId), delete(PATH + "/{id}", artworkId))) {
            mockMvc.perform(request
                            .cookie(user.session(), user.csrf())
                            .header("X-XSRF-TOKEN", user.csrf().getValue())
                            .contentType(MediaType.APPLICATION_JSON).content(PUBLIC_BODY))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value(errorCode));
        }
    }
}
