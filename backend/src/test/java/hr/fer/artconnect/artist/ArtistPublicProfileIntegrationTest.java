package hr.fer.artconnect.artist;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.support.IntegrationTestBase;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ArtistPublicProfileIntegrationTest extends IntegrationTestBase {
    private static final String PATH = "/api/v1/artists/";

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void javnoVraćaProfilIzložbeIPraznaSudjelovanjaBezCsrfTokena() throws Exception {
        UUID artistId = loginAs("ARTIST");
        jdbc.update("INSERT INTO artist_profiles (user_id) VALUES (?)", artistId);
        insertExhibition(artistId, "Starija izložba", 2023, "2025-01-01T10:00:00Z");
        insertExhibition(artistId, "Ranija izložba", 2024, "2025-01-01T10:00:00Z");
        insertExhibition(artistId, "Kasnija izložba", 2024, "2025-01-02T10:00:00Z");

        mockMvc.perform(get(PATH + artistId).with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(artistId.toString()))
                .andExpect(jsonPath("$.biography").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.artFields").isEmpty())
                .andExpect(jsonPath("$.links").isEmpty())
                .andExpect(jsonPath("$.pastExhibitions.length()").value(3))
                .andExpect(jsonPath("$.pastExhibitions[0].title").value("Kasnija izložba"))
                .andExpect(jsonPath("$.pastExhibitions[1].title").value("Ranija izložba"))
                .andExpect(jsonPath("$.pastExhibitions[2].title").value("Starija izložba"))
                .andExpect(jsonPath("$.participations").isEmpty());
    }

    @Test
    void vraća404ZaNepostojećegKorisnikaNeumjetnikaINevidljiveStatuse() throws Exception {
        mockMvc.perform(get(PATH + UUID.randomUUID()).with(anonymous()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));

        UUID organizerId = loginAs("ORGANIZER");
        mockMvc.perform(get(PATH + organizerId).with(anonymous()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));

        for (String hiddenStatus : new String[] {"BLOCKED", "DELETED", "ONBOARDING", "INVITED"}) {
            UUID artistId = loginAs("ARTIST");
            jdbc.update("UPDATE users SET status = ? WHERE id = ?", hiddenStatus, artistId);
            mockMvc.perform(get(PATH + artistId).with(anonymous()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }
    }

    @Test
    void vlasnikIAdministratorMoguVidjetiBlokiranogUmjetnika() throws Exception {
        UUID artistId = loginAs("ARTIST");
        jdbc.update("UPDATE users SET status = 'BLOCKED' WHERE id = ?", artistId);

        mockMvc.perform(get(PATH + artistId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(artistId.toString()));

        loginAs("ADMIN");
        mockMvc.perform(get(PATH + artistId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(artistId.toString()));
    }

    @Test
    void neispravanUuidVraća404() throws Exception {
        mockMvc.perform(get(PATH + "nije-uuid").with(anonymous()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    private void insertExhibition(UUID artistId, String title, int year, String createdAt) {
        jdbc.update("""
                INSERT INTO past_exhibitions (id, artist_id, title, year, place, created_at)
                VALUES (?, ?, ?, ?, 'Zagreb', ?::timestamptz)
                """, UUID.randomUUID(), artistId, title, year, createdAt);
    }
}
