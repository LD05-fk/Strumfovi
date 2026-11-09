package hr.fer.artconnect.file.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.auth.domain.UserRepository;
import hr.fer.artconnect.support.IntegrationTestBase;
import hr.fer.artconnect.support.TestPrincipals;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class FileDownloadIntegrationTest extends IntegrationTestBase {
    private static final String FILES_PATH = "/api/v1/files/";
    private static final String PUBLIC_CACHE = "public, max-age=3600";
    private static final String PRIVATE_CACHE = "private, no-store";
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x01};
    @TempDir
    static Path storageRoot;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    UserRepository users;

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("artconnect.storage.root", () -> storageRoot.toString());
    }

    @Test
    void javnaSlikaDostupnaJeBezPrijaveSIspravnimZaglavljimaIIzvornimSadrzajem() throws Exception {
        UUID artistId = loginAs("ARTIST");
        UUID artworkId = createArtwork(artistId, "PUBLIC", false);
        UUID fileId = createImage(artistId, artworkId, "crtež čć.png");

        byte[] body = mockMvc.perform(get(FILES_PATH + fileId).with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
                .andExpect(header().longValue(HttpHeaders.CONTENT_LENGTH, PNG.length))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, startsWith("inline")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("filename*=UTF-8''crte%C5%BE%20%C4%8D%C4%87.png")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, PUBLIC_CACHE))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(body).isEqualTo(PNG);
    }

    @Test
    void sliku_privatnogSkrivenogIliBlokiranogDjelaVideSamoVlasnikIAdministrator() throws Exception {
        UUID artistId = loginAs("ARTIST");
        UUID otherArtistId = loginAs("ARTIST");
        UUID adminId = loginAs("ADMIN");

        UUID privateImage = createImage(artistId, createArtwork(artistId, "PRIVATE", false), "privatna.png");
        UUID hiddenImage = createImage(artistId, createArtwork(artistId, "PUBLIC", true), "skrivena.png");
        UUID publicImage = createImage(artistId, createArtwork(artistId, "PUBLIC", false), "javna.png");

        for (UUID fileId : new UUID[] {privateImage, hiddenImage}) {
            mockMvc.perform(get(FILES_PATH + fileId).with(anonymous()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
            loginAs(otherArtistId);
            mockMvc.perform(get(FILES_PATH + fileId)).andExpect(status().isNotFound());
            for (UUID viewerId : new UUID[] {artistId, adminId}) {
                loginAs(viewerId);
                mockMvc.perform(get(FILES_PATH + fileId))
                        .andExpect(status().isOk())
                        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, PRIVATE_CACHE));
            }
        }

        // Ista javna slika prestaje biti javno dostupna kad se autora blokira.
        mockMvc.perform(get(FILES_PATH + publicImage).with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, PUBLIC_CACHE));

        jdbc.update("UPDATE users SET status = 'BLOCKED' WHERE id = ?", artistId);
        mockMvc.perform(get(FILES_PATH + publicImage).with(anonymous()))
                .andExpect(status().isNotFound());
        loginAs(otherArtistId);
        mockMvc.perform(get(FILES_PATH + publicImage)).andExpect(status().isNotFound());
        for (UUID viewerId : new UUID[] {artistId, adminId}) {
            loginAs(viewerId);
            mockMvc.perform(get(FILES_PATH + publicImage))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, PRIVATE_CACHE));
        }
    }

    @Test
    void prilogPrijaveVideSamoVlasnikIAdministratorOstalimaJeZabranjen() throws Exception {
        UUID artistId = loginAs("ARTIST");
        UUID otherArtistId = loginAs("ARTIST");
        UUID adminId = loginAs("ADMIN");
        UUID fileId = createAttachment(artistId, "zivotopis.pdf");

        mockMvc.perform(get(FILES_PATH + fileId).with(anonymous()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        loginAs(otherArtistId);
        mockMvc.perform(get(FILES_PATH + fileId))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        for (UUID viewerId : new UUID[] {artistId, adminId}) {
            loginAs(viewerId);
            mockMvc.perform(get(FILES_PATH + fileId))
                    .andExpect(status().isOk())
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"))
                    .andExpect(header().string(HttpHeaders.CACHE_CONTROL, PRIVATE_CACHE));
        }
    }

    @Test
    void neispravanUuidNepostojecaIliNestalaDatotekaVracaju404() throws Exception {
        UUID artistId = loginAs("ARTIST");
        UUID artworkId = createArtwork(artistId, "PUBLIC", false);
        UUID missingFileId = insertImageRow(artistId, artworkId, "nestala.png",
                "artwork_image/" + UUID.randomUUID() + ".png");

        mockMvc.perform(get(FILES_PATH + "nije-uuid").with(anonymous()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        mockMvc.perform(get(FILES_PATH + UUID.randomUUID()).with(anonymous()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        // Zapis u bazi bez datoteke na disku ne smije otkriti da red postoji.
        mockMvc.perform(get(FILES_PATH + missingFileId).with(anonymous()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        mockMvc.perform(get(FILES_PATH + missingFileId))
                .andExpect(status().isNotFound());
    }

    // Prijava postojećeg korisnika, kako bi se u istom testu mogle mijenjati uloge.
    private void loginAs(UUID userId) {
        TestSecurityContextHolder.setAuthentication(
                TestPrincipals.authentication(users.findById(userId).orElseThrow()));
    }

    private UUID createArtwork(UUID artistId, String visibility, boolean hidden) {
        UUID artworkId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO artworks (id, artist_id, title, year, technique, visibility, hidden_by_admin)
                VALUES (?, ?, 'Test djelo', 2024, 'Ulje', ?, ?)
                """, artworkId, artistId, visibility, hidden);
        return artworkId;
    }

    private UUID createImage(UUID ownerId, UUID artworkId, String originalName) throws IOException {
        String storageKey = "artwork_image/" + UUID.randomUUID() + ".png";
        writeToStorage(storageKey);
        return insertImageRow(ownerId, artworkId, originalName, storageKey);
    }

    private UUID insertImageRow(UUID ownerId, UUID artworkId, String originalName, String storageKey) {
        UUID fileId = insertStoredFile(ownerId, "ARTWORK_IMAGE", originalName, "image/png", storageKey);
        jdbc.update("""
                INSERT INTO artwork_images (id, artwork_id, file_id, position, alt_text)
                VALUES (?, ?, ?, 0, 'Test slika')
                """, UUID.randomUUID(), artworkId, fileId);
        return fileId;
    }

    private UUID createAttachment(UUID ownerId, String originalName) throws IOException {
        String storageKey = "application_attachment/" + UUID.randomUUID() + ".pdf";
        writeToStorage(storageKey);
        return insertStoredFile(ownerId, "APPLICATION_ATTACHMENT", originalName,
                "application/pdf", storageKey);
    }

    private UUID insertStoredFile(UUID ownerId, String purpose, String originalName,
            String contentType, String storageKey) {
        UUID fileId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO stored_files
                    (id, owner_id, purpose, original_name, content_type, size_bytes, storage_key)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, fileId, ownerId, purpose, originalName, contentType, PNG.length, storageKey);
        return fileId;
    }

    private void writeToStorage(String storageKey) throws IOException {
        Path path = storageRoot.resolve(storageKey);
        Files.createDirectories(path.getParent());
        Files.write(path, PNG);
    }
}
