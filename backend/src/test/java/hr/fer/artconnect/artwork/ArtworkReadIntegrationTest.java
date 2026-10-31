package hr.fer.artconnect.artwork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.support.IntegrationTestBase;
import jakarta.persistence.EntityManagerFactory;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class ArtworkReadIntegrationTest extends IntegrationTestBase {
    private static final String ARTISTS_PATH = "/api/v1/artists/";
    private static final String ARTWORKS_PATH = "/api/v1/artworks/";

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    @Test
    void posjetiteljDobivaSamoJavnaNeskrivenaDjelaISlike() throws Exception {
        UUID artistId = loginAs("ARTIST");
        UUID visibleId = createArtwork(artistId, "PUBLIC", false);
        addImages(artistId, visibleId, 3);
        createArtwork(artistId, "PRIVATE", false);
        createArtwork(artistId, "PUBLIC", true);

        mockMvc.perform(get(ARTISTS_PATH + artistId + "/artworks").with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(visibleId.toString()))
                .andExpect(jsonPath("$.data[0].images.length()").value(3))
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.pageSize").value(20))
                .andExpect(jsonPath("$.meta.total").value(1));

        mockMvc.perform(get(ARTWORKS_PATH + visibleId).with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(visibleId.toString()))
                .andExpect(jsonPath("$.images.length()").value(3));
    }

    @Test
    void vlasnikIVidiSvaDjelaDrugiUmjetnikSamoJavnaAAdministratorSva() throws Exception {
        UUID artistId = loginAs("ARTIST");
        UUID publicId = createArtwork(artistId, "PUBLIC", false);
        UUID privateId = createArtwork(artistId, "PRIVATE", false);
        UUID hiddenId = createArtwork(artistId, "PUBLIC", true);

        mockMvc.perform(get(ARTISTS_PATH + artistId + "/artworks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(3));

        loginAs("ARTIST");
        mockMvc.perform(get(ARTISTS_PATH + artistId + "/artworks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(1))
                .andExpect(jsonPath("$.data[0].id").value(publicId.toString()));
        mockMvc.perform(get(ARTWORKS_PATH + privateId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        mockMvc.perform(get(ARTWORKS_PATH + hiddenId))
                .andExpect(status().isNotFound());

        loginAs("ADMIN");
        mockMvc.perform(get(ARTISTS_PATH + artistId + "/artworks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(3));
        mockMvc.perform(get(ARTWORKS_PATH + privateId))
                .andExpect(status().isOk());
    }

    @Test
    void blokiranIObrisanUmjetnikNisuJavnoDostupniAliDostupniSuVlasnikuIAdminu() throws Exception {
        UUID artistId = loginAs("ARTIST");
        UUID artworkId = createArtwork(artistId, "PUBLIC", false);
        jdbc.update("UPDATE users SET status = 'BLOCKED' WHERE id = ?", artistId);

        mockMvc.perform(get(ARTISTS_PATH + artistId + "/artworks").with(anonymous()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        mockMvc.perform(get(ARTWORKS_PATH + artworkId).with(anonymous()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(ARTWORKS_PATH + artworkId))
                .andExpect(status().isOk());

        loginAs("ADMIN");
        mockMvc.perform(get(ARTISTS_PATH + artistId + "/artworks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").value(1));

        jdbc.update("UPDATE users SET status = 'DELETED' WHERE id = ?", artistId);
        mockMvc.perform(get(ARTWORKS_PATH + artworkId).with(anonymous()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(ARTWORKS_PATH + artworkId))
                .andExpect(status().isOk());
    }

    @Test
    void stranicaBezDjelaJePraznaANepostojeciIliNeumjetnikVraca404() throws Exception {
        UUID artistId = loginAs("ARTIST");
        mockMvc.perform(get(ARTISTS_PATH + artistId + "/artworks").with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta.total").value(0));

        UUID organizerId = loginAs("ORGANIZER");
        mockMvc.perform(get(ARTISTS_PATH + organizerId + "/artworks").with(anonymous()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        mockMvc.perform(get(ARTISTS_PATH + UUID.randomUUID() + "/artworks").with(anonymous()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(ARTWORKS_PATH + UUID.randomUUID()).with(anonymous()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void neispravniParametriStranicenjaVracajuValidationError() throws Exception {
        UUID artistId = loginAs("ARTIST");
        mockMvc.perform(get(ARTISTS_PATH + artistId + "/artworks?page=0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get(ARTISTS_PATH + artistId + "/artworks?pageSize=101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get(ARTISTS_PATH + artistId + "/artworks?pageSize=abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void brojSqlUpitaZaSlikeNeRasteSBrojemDjelaNaStranici() throws Exception {
        UUID artistId = loginAs("ARTIST");
        UUID oneArtwork = createArtwork(artistId, "PUBLIC", false);
        addImages(artistId, oneArtwork, 3);
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        var statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);

        statistics.clear();
        mockMvc.perform(get(ARTISTS_PATH + artistId + "/artworks?pageSize=1").with(anonymous()))
                .andExpect(status().isOk());
        long oneArtworkQueryCount = statistics.getPrepareStatementCount();

        for (int i = 0; i < 19; i++) {
            UUID artworkId = createArtwork(artistId, "PUBLIC", false);
            addImages(artistId, artworkId, 3);
        }

        statistics.clear();
        mockMvc.perform(get(ARTISTS_PATH + artistId + "/artworks?pageSize=20").with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(20));
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(oneArtworkQueryCount);
    }

    private UUID createArtwork(UUID artistId, String visibility, boolean hidden) {
        UUID artworkId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO artworks (id, artist_id, title, year, technique, visibility, hidden_by_admin)
                VALUES (?, ?, 'Test artwork', 2024, 'Oil', ?, ?)
                """, artworkId, artistId, visibility, hidden);
        return artworkId;
    }

    private void addImages(UUID artistId, UUID artworkId, int count) {
        for (int position = 0; position < count; position++) {
            UUID fileId = UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO stored_files
                        (id, owner_id, purpose, original_name, content_type, size_bytes, storage_key)
                    VALUES (?, ?, 'ARTWORK_IMAGE', ?, 'image/jpeg', 1234, ?)
                    """, fileId, artistId, "artwork-" + fileId + ".jpg", fileId.toString());
            jdbc.update("""
                    INSERT INTO artwork_images (id, artwork_id, file_id, position, alt_text)
                    VALUES (?, ?, ?, ?, 'Test image')
                    """, UUID.randomUUID(), artworkId, fileId, position);
        }
    }
}
