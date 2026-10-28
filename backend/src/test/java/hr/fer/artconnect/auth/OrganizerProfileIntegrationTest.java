package hr.fer.artconnect.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.support.IntegrationTestBase;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.TestSecurityContextHolder;

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
    void putObrezujeNazivAzuriraProfilIVrijemeAzuriranja() throws Exception {
        UUID organizerId = loginAs("ORGANIZER");
        jdbc.update("INSERT INTO organizer_profiles (user_id, organization_name, updated_at) VALUES (?, ?, ?)",
                organizerId, "Stari naziv", OffsetDateTime.parse("2000-01-01T00:00:00Z"));
        var oldUpdatedAt = jdbc.queryForObject(
                "SELECT updated_at FROM organizer_profiles WHERE user_id = ?", OffsetDateTime.class, organizerId);

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
                "SELECT updated_at FROM organizer_profiles WHERE user_id = ?", OffsetDateTime.class, organizerId))
                .isAfter(oldUpdatedAt);
    }

    @Test
    void dvaIstodobnaPrvaPutStvarajuJedanProfil() throws Exception {
        UUID organizerId = loginAs("ORGANIZER");
        Authentication auth = TestSecurityContextHolder.getContext().getAuthentication();
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> updateProfile(auth, start, "Galerija Prva"));
            var second = executor.submit(() -> updateProfile(auth, start, "Galerija Druga"));
            start.countDown();
            assertThat(new Integer[] {first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS)})
                    .containsExactly(200, 200);
        }

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM organizer_profiles WHERE user_id = ?", Integer.class, organizerId)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT organization_name FROM organizer_profiles WHERE user_id = ?", String.class, organizerId))
                .isIn("Galerija Prva", "Galerija Druga");
    }

    private int updateProfile(Authentication auth, CountDownLatch start, String name) throws Exception {
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Test nije pokrenuo istodobne zahtjeve.");
        }
        return mockMvc.perform(put(PATH).with(authentication(auth)).with(withCsrf())
                        .contentType("application/json")
                        .content("{\"organizationName\":\"" + name + "\"}"))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void izostavljenoPoljePrazanNizINullBrisuNaziv() throws Exception {
        UUID organizerId = loginAs("ORGANIZER");
        jdbc.update("INSERT INTO organizer_profiles (user_id, organization_name) VALUES (?, ?)",
                organizerId, "Postojeći naziv");
        var oldUpdatedAt = jdbc.queryForObject(
                "SELECT updated_at FROM organizer_profiles WHERE user_id = ?", OffsetDateTime.class,
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
                "SELECT updated_at FROM organizer_profiles WHERE user_id = ?", OffsetDateTime.class,
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
