package hr.fer.artconnect.artist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.support.IntegrationTestBase;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ArtistProfileIntegrationTest extends IntegrationTestBase {
    private static final String PATH = "/api/v1/me/artist-profile";
    private static final UUID INACTIVE_FIELD_ID = UUID.randomUUID();
    private static final String INACTIVE_FIELD_CODE = "test-neaktivno-" + INACTIVE_FIELD_ID.toString().substring(0, 8);

    @Autowired
    JdbcTemplate jdbc;

    @AfterEach
    void removeInactiveTestArtField() {
        jdbc.update("DELETE FROM artist_profile_art_fields WHERE art_field_id = ?", INACTIVE_FIELD_ID);
        jdbc.update("DELETE FROM art_fields WHERE id = ?", INACTIVE_FIELD_ID);
    }

    @Test
    void vracaProfilPodrucjaIPoveznicePoredanePoPoziciji() throws Exception {
        UUID artistId = loginAs("ARTIST");
        jdbc.update("UPDATE users SET avatar_url = ? WHERE id = ?", "https://example.test/avatar.png", artistId);
        jdbc.update("""
                INSERT INTO artist_profiles (user_id, biography, artist_statement)
                VALUES (?, 'Biografija', 'Umjetnička izjava')
                """, artistId);
        jdbc.update("""
                INSERT INTO artist_profile_art_fields (user_id, art_field_id)
                SELECT ?, id FROM art_fields WHERE code IN ('slikarstvo', 'fotografija')
                """, artistId);
        insertInactiveArtField(artistId);
        insertLink(artistId, "00000000-0000-0000-0000-000000000003", "Kasnije", "https://example.test/later", 2);
        insertLink(artistId, "00000000-0000-0000-0000-000000000002", null, "https://example.test/tie-b", 1);
        insertLink(artistId, "00000000-0000-0000-0000-000000000001", "Prva", "https://example.test/first", 1);
        List<String> expectedArtFields = jdbc.queryForList("""
                SELECT field.code
                FROM artist_profile_art_fields profile_field
                JOIN art_fields field ON field.id = profile_field.art_field_id
                WHERE profile_field.user_id = ?
                ORDER BY field.position, field.id
                """, String.class, artistId);
        assertThat(expectedArtFields).contains(INACTIVE_FIELD_CODE);
        assertThat(jdbc.queryForObject(
                "SELECT active FROM art_fields WHERE id = ?", Boolean.class, INACTIVE_FIELD_ID)).isFalse();

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(artistId.toString()))
                .andExpect(jsonPath("$.displayName").value("Test ARTIST"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.test/avatar.png"))
                .andExpect(jsonPath("$.biography").value("Biografija"))
                .andExpect(jsonPath("$.artistStatement").value("Umjetnička izjava"))
                .andExpect(jsonPath("$.artFields.length()").value(3))
                .andExpect(jsonPath("$.artFields[0].code").value(expectedArtFields.get(0)))
                .andExpect(jsonPath("$.artFields[1].code").value(expectedArtFields.get(1)))
                .andExpect(jsonPath("$.artFields[2].code").value(expectedArtFields.get(2)))
                .andExpect(jsonPath("$.links.length()").value(3))
                .andExpect(jsonPath("$.links[0].label").value("Prva"))
                .andExpect(jsonPath("$.links[1].label").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.links[1].url").value("https://example.test/tie-b"))
                .andExpect(jsonPath("$.links[2].label").value("Kasnije"));
    }

    @Test
    void vracaPrazanProfilBezStvaranjaReda() throws Exception {
        UUID artistId = loginAs("ARTIST");

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(artistId.toString()))
                .andExpect(jsonPath("$.biography").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.artistStatement").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.artFields").isEmpty())
                .andExpect(jsonPath("$.links").isEmpty());

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM artist_profiles WHERE user_id = ?", Long.class, artistId)).isZero();
    }

    @Test
    void neprijavljenomVracaUnauthenticated() throws Exception {
        mockMvc.perform(get(PATH).with(anonymous()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void korisnicimaKojiNisuUmjetniciVracaForbidden() throws Exception {
        for (String role : List.of("ORGANIZER", "ADMIN")) {
            loginAs(role);
            mockMvc.perform(get(PATH))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        }
    }

    @Test
    void korisnikuKojiJosNijeOdabraoUloguVracaOnboardingRequired() throws Exception {
        UUID artistId = loginAs("ARTIST");
        jdbc.update("UPDATE users SET status = 'ONBOARDING', role = NULL WHERE id = ?", artistId);

        mockMvc.perform(get(PATH))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ONBOARDING_REQUIRED"));
    }

    @Test
    void blokiranomUmjetnikuVracaUserBlocked() throws Exception {
        UUID artistId = loginAs("ARTIST");
        jdbc.update("UPDATE users SET status = 'BLOCKED' WHERE id = ?", artistId);

        mockMvc.perform(get(PATH))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("USER_BLOCKED"));
    }

    private void insertLink(UUID artistId, String id, String label, String url, int position) {
        jdbc.update("""
                INSERT INTO artist_links (id, user_id, label, url, position)
                VALUES (?::uuid, ?, ?, ?, ?)
                """, id, artistId, label, url, position);
    }

    private void insertInactiveArtField(UUID artistId) {
        jdbc.update("""
                INSERT INTO art_fields (id, code, name, active, position)
                VALUES (?, ?, 'Testno neaktivno područje', false, 3)
                """, INACTIVE_FIELD_ID, INACTIVE_FIELD_CODE);
        jdbc.update("""
                INSERT INTO artist_profile_art_fields (user_id, art_field_id)
                VALUES (?, ?)
                """, artistId, INACTIVE_FIELD_ID);
    }
}
