package hr.fer.artconnect.portfolio.it;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.portfolio.it.PortfolioFixtures.UserSession;
import hr.fer.artconnect.common.TimeConfiguration;
import hr.fer.artconnect.support.IntegrationTestBase;
import java.time.Year;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

/** Provjerava cijeli tok kroz sigurnosne filtre, kontroler, servis i PostgreSQL. */
@ActiveProfiles("dev")
class ArtistProfileFlowIT extends IntegrationTestBase {
    private static final String PATH = "/api/v1/me/artist-profile";
    private static final String EXHIBITIONS = "/api/v1/past-exhibitions";
    private static final String EXHIBITION_BODY = """
            {"title":"Gradski motivi","year":2024,"place":"Zagreb"}
            """;
    private static final String EMPTY_PROFILE = """
            {"biography":null,"artistStatement":null,"artFieldIds":[],"links":[]}
            """;

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
    }

    @Test
    void umjetnikDohvacaPrazanProfilSpremaGaIPonovnoCita() throws Exception {
        UUID artistId = fixtures.user("ARTIST", "ACTIVE");
        var user = fixtures.login(artistId);
        UUID firstField = fixtures.artField();
        UUID secondField = fixtures.artField();

        mockMvc.perform(get(PATH).cookie(user.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(artistId.toString()))
                .andExpect(jsonPath("$.artFields").isEmpty())
                .andExpect(jsonPath("$.links").isEmpty());

        save(user, """
                {"biography":"Biografija umjetnika","artistStatement":"Opis rada",
                 "artFieldIds":["%s","%s"],
                 "links":[{"label":"Instagram","url":"https://instagram.com/test"}]}
                """.formatted(firstField, secondField))
                .andExpect(status().isOk());

        // Novi zahtjev s istom sesijom mora pročitati podatke spremljene u bazu.
        mockMvc.perform(get(PATH).cookie(user.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.biography").value("Biografija umjetnika"))
                .andExpect(jsonPath("$.artistStatement").value("Opis rada"))
                .andExpect(jsonPath("$.artFields[*].id").value(org.hamcrest.Matchers.containsInAnyOrder(
                        firstField.toString(), secondField.toString())))
                .andExpect(jsonPath("$.links.length()").value(1))
                .andExpect(jsonPath("$.links[0].label").value("Instagram"))
                .andExpect(jsonPath("$.links[0].url").value("https://instagram.com/test"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://x", "javascript:alert(1)"})
    void odbijaNedopusteniProtokolBezSpremanjaProfila(String url) throws Exception {
        var user = fixtures.login(fixtures.user("ARTIST", "ACTIVE"));

        save(user, """
                {"biography":null,"artistStatement":null,"artFieldIds":[],
                 "links":[{"label":null,"url":"%s"}]}
                """.formatted(url))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("links[0].url"));

        mockMvc.perform(get(PATH).cookie(user.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.links").isEmpty());
    }

    @Test
    void nakonDeaktivacijePodrucjeOstajePostojecemUmjetnikuAliSeNeMozeDodatiDrugome() throws Exception {
        var owner = fixtures.login(fixtures.user("ARTIST", "ACTIVE"));
        UUID fieldId = fixtures.artField();
        String body = """
                {"biography":null,"artistStatement":null,"artFieldIds":["%s"],"links":[]}
                """.formatted(fieldId);
        save(owner, body).andExpect(status().isOk());
        jdbc.update("UPDATE art_fields SET active = false WHERE id = ?", fieldId);

        save(owner, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artFields[0].id").value(fieldId.toString()));

        var other = fixtures.login(fixtures.user("ARTIST", "ACTIVE"));
        save(other, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("artFieldIds"));
    }

    @ParameterizedTest
    @CsvSource({
            "ORGANIZER, ACTIVE, FORBIDDEN",
            "ADMIN, ACTIVE, FORBIDDEN",
            ", ONBOARDING, ONBOARDING_REQUIRED",
            "ARTIST, BLOCKED, USER_BLOCKED"
    })
    void odbijaSpremanjeBezPravaPristupa(String role, String userStatus, String errorCode) throws Exception {
        var user = fixtures.login(fixtures.user(role, userStatus));

        save(user, EMPTY_PROFILE)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(errorCode));
    }

    @Test
    void umjetnikDodajeUredujeIBriseIzlozbuPromjeneSuVidljiveNaJavnomProfilu() throws Exception {
        UUID artistId = fixtures.user("ARTIST", "ACTIVE");
        var user = fixtures.login(artistId);

        String response = mockMvc.perform(post(EXHIBITIONS)
                        .cookie(user.session(), user.csrf())
                        .header("X-XSRF-TOKEN", user.csrf().getValue())
                        .contentType(MediaType.APPLICATION_JSON).content(EXHIBITION_BODY))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(response).get("id").asText();

        // Čitamo javni profil novim zahtjevom: izložba mora biti stvarno spremljena.
        mockMvc.perform(get("/api/v1/artists/{id}", artistId).cookie(user.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pastExhibitions.length()").value(1))
                .andExpect(jsonPath("$.pastExhibitions[0].id").value(id))
                .andExpect(jsonPath("$.pastExhibitions[0].title").value("Gradski motivi"))
                .andExpect(jsonPath("$.pastExhibitions[0].year").value(2024))
                .andExpect(jsonPath("$.pastExhibitions[0].place").value("Zagreb"));

        mockMvc.perform(put(EXHIBITIONS + "/{id}", id)
                        .cookie(user.session(), user.csrf())
                        .header("X-XSRF-TOKEN", user.csrf().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EXHIBITION_BODY.replace("Zagreb", "Split")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/artists/{id}", artistId).cookie(user.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pastExhibitions.length()").value(1))
                .andExpect(jsonPath("$.pastExhibitions[0].id").value(id))
                .andExpect(jsonPath("$.pastExhibitions[0].place").value("Split"));

        mockMvc.perform(delete(EXHIBITIONS + "/{id}", id)
                        .cookie(user.session(), user.csrf())
                        .header("X-XSRF-TOKEN", user.csrf().getValue()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/artists/{id}", artistId).cookie(user.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pastExhibitions").isEmpty());
    }

    @Test
    void odbijaIzlozbuSljedeceGodineBezSpremanja() throws Exception {
        UUID artistId = fixtures.user("ARTIST", "ACTIVE");
        var user = fixtures.login(artistId);
        // Granica se računa prema satu aplikacije, pa test vrijedi i iduće godine.
        int nextYear = Year.now(TimeConfiguration.APP_ZONE).getValue() + 1;

        mockMvc.perform(post(EXHIBITIONS)
                        .cookie(user.session(), user.csrf())
                        .header("X-XSRF-TOKEN", user.csrf().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EXHIBITION_BODY.replace("2024", String.valueOf(nextYear))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("year"));
        mockMvc.perform(get("/api/v1/artists/{id}", artistId).cookie(user.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pastExhibitions").isEmpty());
    }

    @Test
    void drugiUmjetnikNeMozeUreditiTuduIzlozbu() throws Exception {
        UUID ownerId = fixtures.user("ARTIST", "ACTIVE");
        UUID exhibitionId = fixtures.pastExhibition(ownerId);
        var other = fixtures.login(fixtures.user("ARTIST", "ACTIVE"));

        mockMvc.perform(put(EXHIBITIONS + "/{id}", exhibitionId)
                        .cookie(other.session(), other.csrf())
                        .header("X-XSRF-TOKEN", other.csrf().getValue())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EXHIBITION_BODY.replace("Zagreb", "Split")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));

        // Odbijeni zahtjev ne smije promijeniti vlasnikove podatke.
        mockMvc.perform(get("/api/v1/artists/{id}", ownerId).cookie(other.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pastExhibitions[0].place").value("Zagreb"));
    }

    @ParameterizedTest
    @CsvSource({"ORGANIZER, ACTIVE, FORBIDDEN", ", ONBOARDING, ONBOARDING_REQUIRED"})
    void odbijaSvePromjeneIzlozbiBezPrava(String role, String userStatus, String errorCode) throws Exception {
        UUID ownerId = fixtures.user("ARTIST", "ACTIVE");
        UUID exhibitionId = fixtures.pastExhibition(ownerId);
        var user = fixtures.login(fixtures.user(role, userStatus));

        // Isti korisnik mora biti odbijen za sve tri radnje, uz valjanu sesiju i CSRF.
        var requests = java.util.List.of(
                post(EXHIBITIONS),
                put(EXHIBITIONS + "/{id}", exhibitionId),
                delete(EXHIBITIONS + "/{id}", exhibitionId));
        for (var request : requests) {
            mockMvc.perform(request
                            .cookie(user.session(), user.csrf())
                            .header("X-XSRF-TOKEN", user.csrf().getValue())
                            .contentType(MediaType.APPLICATION_JSON).content(EXHIBITION_BODY))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value(errorCode));
        }
    }

    private ResultActions save(UserSession user, String body) throws Exception {
        return mockMvc.perform(put(PATH)
                .cookie(user.session(), user.csrf())
                .header("X-XSRF-TOKEN", user.csrf().getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
