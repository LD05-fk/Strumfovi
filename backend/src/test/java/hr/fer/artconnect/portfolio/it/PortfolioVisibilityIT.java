package hr.fer.artconnect.portfolio.it;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.portfolio.it.PortfolioFixtures.UserSession;
import hr.fer.artconnect.support.IntegrationTestBase;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Provjerava javni prikaz spremljenih podataka profila. */
@ActiveProfiles("dev")
class PortfolioVisibilityIT extends IntegrationTestBase {
    private static final String PROFILE_PATH = "/api/v1/me/artist-profile";
    private static final String PUBLIC_PROFILE_PATH = "/api/v1/artists/";

    @Autowired
    JdbcTemplate jdbc;

    private PortfolioFixtures fixtures;

    @BeforeEach
    void prepareFixtures() {
        fixtures = new PortfolioFixtures(jdbc, mockMvc);
    }

    @AfterEach
    void removeFixtures() {
        fixtures.cleanup();
    }

    @Test
    void javniProfilVraćaPodručjaIPovezniceRedomKojimSuSpremljeni() throws Exception {
        UUID artistId = fixtures.user("ARTIST", "ACTIVE");
        UserSession artist = fixtures.login(artistId);
        UUID firstField = fixtures.artField();
        UUID secondField = fixtures.artField();
        UUID thirdField = fixtures.artField();

        mockMvc.perform(put(PROFILE_PATH)
                        .cookie(artist.session(), artist.csrf())
                        .header("X-XSRF-TOKEN", artist.csrf().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"biography":"Javna biografija","artistStatement":"Opis rada",
                                 "artFieldIds":["%s","%s","%s"],
                                 "links":[
                                   {"label":"Portfolio","url":"https://portfolio.example.test"},
                                   {"label":"Instagram","url":"https://instagram.com/portfolio-test"}
                                 ]}
                                """.formatted(firstField, secondField, thirdField)))
                .andExpect(status().isOk());

        mockMvc.perform(get(PUBLIC_PROFILE_PATH + artistId).with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biography").value("Javna biografija"))
                .andExpect(jsonPath("$.artFields.length()").value(3))
                .andExpect(jsonPath("$.artFields[0].id").value(firstField.toString()))
                .andExpect(jsonPath("$.artFields[1].id").value(secondField.toString()))
                .andExpect(jsonPath("$.artFields[2].id").value(thirdField.toString()))
                .andExpect(jsonPath("$.links.length()").value(2))
                .andExpect(jsonPath("$.links[0].label").value("Portfolio"))
                .andExpect(jsonPath("$.links[0].url").value("https://portfolio.example.test"))
                .andExpect(jsonPath("$.links[1].label").value("Instagram"))
                .andExpect(jsonPath("$.links[1].url").value("https://instagram.com/portfolio-test"))
                .andExpect(jsonPath("$.participations").isEmpty());
    }

    @Test
    void blokiraniUmjetnikNemaJavniProfil() throws Exception {
        UUID blockedArtistId = fixtures.user("ARTIST", "BLOCKED");

        mockMvc.perform(get(PUBLIC_PROFILE_PATH + blockedArtistId).with(anonymous()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }
}
