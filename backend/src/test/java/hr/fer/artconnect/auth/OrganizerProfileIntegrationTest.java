package hr.fer.artconnect.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.support.IntegrationTestBase;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class OrganizerProfileIntegrationTest extends IntegrationTestBase {
    private static final String PATH = "/api/v1/me/organizer-profile";

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void vracaProfilPostojecegOrganizatora() throws Exception {
        UUID organizerId = loginAs("ORGANIZER");
        jdbc.update("UPDATE users SET display_name = ?, organizer_approval = ? WHERE id = ?",
                "Marko Kovačević", "PENDING", organizerId);
        jdbc.update("INSERT INTO organizer_profiles (user_id, organization_name) VALUES (?, ?)",
                organizerId, "Galerija Kontrast");

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(organizerId.toString()))
                .andExpect(jsonPath("$.displayName").value("Marko Kovačević"))
                .andExpect(jsonPath("$.organizationName").value("Galerija Kontrast"))
                .andExpect(jsonPath("$.organizerApproval").value("PENDING"));
    }

    @Test
    void profilBezRetkaVracaNullIneStvaraRed() throws Exception {
        UUID organizerId = loginAs("ORGANIZER");

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationName").value(org.hamcrest.Matchers.nullValue()));

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM organizer_profiles WHERE user_id = ?", Long.class, organizerId)).isZero();
    }

    @Test
    void neprijavljenomVracaUnauthenticatedZaObaEndpointa() throws Exception {
        mockMvc.perform(get(PATH).with(anonymous()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        mockMvc.perform(put(PATH).with(anonymous()).with(withCsrf()).contentType("application/json")
                .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void korisnicimaKojiNisuOrganizatoriVracaForbiddenZaObaEndpointa() throws Exception {
        for (String role : List.of("ARTIST", "ADMIN")) {
            loginAs(role);
            mockMvc.perform(get(PATH))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
            mockMvc.perform(put(PATH).with(withCsrf()).contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        }
    }

    @Test
    void onboardingKorisnikuVracaOnboardingRequired() throws Exception {
        UUID organizerId = loginAs("ORGANIZER");
        jdbc.update("UPDATE users SET status = 'ONBOARDING', role = NULL, organizer_approval = NULL WHERE id = ?",
                organizerId);

        mockMvc.perform(get(PATH))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ONBOARDING_REQUIRED"));
        mockMvc.perform(put(PATH).with(withCsrf()).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ONBOARDING_REQUIRED"));
    }

    @Test
    void blokiranomOrganizatoruVracaUserBlocked() throws Exception {
        UUID organizerId = loginAs("ORGANIZER");
        jdbc.update("UPDATE users SET status = 'BLOCKED' WHERE id = ?", organizerId);

        mockMvc.perform(get(PATH))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("USER_BLOCKED"));
        mockMvc.perform(put(PATH).with(withCsrf()).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("USER_BLOCKED"));
    }

    @Test
    void putObrezujeNazivUpsertaProfilIMijenjaVrijemeAzuriranja() throws Exception {
        UUID organizerId = loginAs("ORGANIZER");

        mockMvc.perform(put(PATH).with(withCsrf()).contentType("application/json")
                .content("{\"organizationName\":\"  Galerija Kontrast  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(organizerId.toString()))
                .andExpect(jsonPath("$.organizationName").value("Galerija Kontrast"))
                .andExpect(jsonPath("$.organizerApproval").value("APPROVED"));

        assertThat(jdbc.queryForObject(
                "SELECT organization_name FROM organizer_profiles WHERE user_id = ?", String.class, organizerId))
                .isEqualTo("Galerija Kontrast");
        assertThat(jdbc.queryForObject(
                "SELECT updated_at FROM organizer_profiles WHERE user_id = ?", java.time.OffsetDateTime.class,
                organizerId)).isNotNull();
    }

    @Test
    void izostavljenoPoljePrazanNizINullBrisuNaziv() throws Exception {
        UUID organizerId = loginAs("ORGANIZER");
        jdbc.update("INSERT INTO organizer_profiles (user_id, organization_name) VALUES (?, ?)",
                organizerId, "Postojeći naziv");
        var oldUpdatedAt = jdbc.queryForObject(
                "SELECT updated_at FROM organizer_profiles WHERE user_id = ?", java.time.OffsetDateTime.class,
                organizerId);

        mockMvc.perform(put(PATH).with(withCsrf()).contentType("application/json")
                .content("{\"organizationName\":\"   \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationName").value(org.hamcrest.Matchers.nullValue()));
        mockMvc.perform(put(PATH).with(withCsrf()).contentType("application/json")
                .content("{\"organizationName\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationName").value(org.hamcrest.Matchers.nullValue()));
        mockMvc.perform(put(PATH).with(withCsrf()).contentType("application/json").content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.organizationName").value(org.hamcrest.Matchers.nullValue()));

        assertThat(jdbc.queryForObject(
                "SELECT organization_name FROM organizer_profiles WHERE user_id = ?", String.class, organizerId))
                .isNull();
        assertThat(jdbc.queryForObject(
                "SELECT updated_at FROM organizer_profiles WHERE user_id = ?", java.time.OffsetDateTime.class,
                organizerId)).isAfter(oldUpdatedAt);
    }

    @Test
    void nazivDuljiOdDvjestotinjeZnakovaVracaValidationError() throws Exception {
        loginAs("ORGANIZER");
        String tooLong = "a".repeat(201);

        mockMvc.perform(put(PATH).with(withCsrf()).contentType("application/json")
                .content("{\"organizationName\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("organizationName"));
    }

    @Test
    void putBezCsrfTokenaVracaForbidden() throws Exception {
        loginAs("ORGANIZER");

        mockMvc.perform(put(PATH).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }
}
