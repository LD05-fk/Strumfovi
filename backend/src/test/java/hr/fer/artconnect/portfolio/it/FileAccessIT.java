package hr.fer.artconnect.portfolio.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.support.IntegrationTestBase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.ObjectMapper;

/** PORT-04: stvarno učitana slika i pristup datoteci kroz spremljene korisničke sesije. */
@ActiveProfiles("dev")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FileAccessIT extends IntegrationTestBase {
    @TempDir
    static Path storageRoot;

    // Spring i servis pohrane od početka koriste samo privremenu mapu ove klase.
    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("artconnect.storage.root", () -> storageRoot.toString());
    }

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    ObjectMapper objectMapper;

    private PortfolioFixtures fixtures;

    @BeforeEach
    void prepareFixtures() {
        fixtures = new PortfolioFixtures(jdbc, mockMvc);
    }

    @AfterEach
    void removeFixtures() {
        fixtures.cleanup();
        // JUnit uklanja @TempDir i kod pada testa, uključujući spremljene slike.
    }

    @Test
    void javneSlikeSePosluzujuAPrivatneVideSamoVlasnikIAdministrator() throws Exception {
        UUID artistId = fixtures.user("ARTIST", "ACTIVE");
        UUID artworkId = fixtures.artwork(artistId, OffsetDateTime.now());
        var owner = fixtures.login(artistId);
        var other = fixtures.login(fixtures.user("ARTIST", "ACTIVE"));
        var admin = fixtures.login(fixtures.user("ADMIN", "ACTIVE"));

        // Oba formata stvarno učitavamo. GET mora vratiti izvorne bajtove i pravi tip.
        for (String extension : new String[] {"jpg", "png"}) {
            byte[] image = Files.readAllBytes(Path.of("src/test/resources/files/small." + extension));
            String response = fixtures.uploadImage(artworkId, owner, "art." + extension, image)
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            String url = objectMapper.readTree(response).get("file").get("url").asText();
            mockMvc.perform(get(url))
                    .andExpect(status().isOk())
                    .andExpect(content().bytes(image))
                    .andExpect(content().contentType(extension.equals("jpg") ? "image/jpeg" : "image/png"))
                    .andExpect(header().longValue("Content-Length", image.length))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().string("Cache-Control", "public, max-age=3600"));
        }

        // Vlasnik mijenja vidljivost putem API-ja; ista URL adresa slike više nije javna.
        mockMvc.perform(put("/api/v1/artworks/{id}", artworkId)
                        .cookie(owner.session(), owner.csrf())
                        .header("X-XSRF-TOKEN", owner.csrf().getValue())
                        .contentType("application/json")
                        .content("""
                                {"title":"Privatni motivi","year":2024,"technique":"Akril",
                                 "visibility":"PRIVATE"}
                                """))
                .andExpect(status().isOk());
        var files = jdbc.queryForList(
                "SELECT file_id FROM artwork_images WHERE artwork_id = ?", UUID.class, artworkId);
        assertThat(files).hasSize(2);
        for (UUID fileId : files) {
            String url = "/api/v1/files/" + fileId;
            mockMvc.perform(get(url)).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
            mockMvc.perform(get(url).cookie(other.session())).andExpect(status().isNotFound());
            for (var viewer : new PortfolioFixtures.UserSession[] {owner, admin}) {
                mockMvc.perform(get(url).cookie(viewer.session()))
                        .andExpect(status().isOk())
                        .andExpect(header().string("Cache-Control", "private, no-store"));
            }
        }
    }
}
