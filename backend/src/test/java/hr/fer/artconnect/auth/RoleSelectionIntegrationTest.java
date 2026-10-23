package hr.fer.artconnect.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.auth.domain.UserRepository;
import hr.fer.artconnect.support.IntegrationTestBase;
import hr.fer.artconnect.support.TestPrincipals;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;

class RoleSelectionIntegrationTest extends IntegrationTestBase {
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;

    @ParameterizedTest
    @ValueSource(strings = {"ARTIST", "ORGANIZER"})
    void odabirSpremaPodatkeIProfilADrugiPozivVraca409(String role) throws Exception {
        UUID id = createOnboardingUser();
        var auth = TestPrincipals.authentication(users.findById(id).orElseThrow());

        mockMvc.perform(post("/api/v1/me/role").with(authentication(auth)).with(withCsrf())
                        .contentType("application/json").content("{\"role\":\"" + role + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(role))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.organizerApproval").value(
                        "ORGANIZER".equals(role) ? "APPROVED" : null));

        var user = users.findById(id).orElseThrow();
        assertThat(user.getRole().name()).isEqualTo(role);
        assertThat(user.getStatus().name()).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM organizer_profiles WHERE user_id = ?",
                Integer.class, id)).isEqualTo("ORGANIZER".equals(role) ? 1 : 0);
        if ("ORGANIZER".equals(role)) {
            assertThat(jdbc.queryForObject("SELECT organization_name FROM organizer_profiles WHERE user_id = ?",
                    String.class, id)).isNull();
        }

        mockMvc.perform(post("/api/v1/me/role").with(authentication(auth)).with(withCsrf())
                        .contentType("application/json").content("{\"role\":\"ARTIST\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ROLE_ALREADY_CHOSEN"));
        assertThat(users.findById(id).orElseThrow().getRole().name()).isEqualTo(role);
    }

    @Test
    void dvaIstodobnaOdabiraStvarajuSamoJedanProfil() throws Exception {
        UUID id = createOnboardingUser();
        var auth = TestPrincipals.authentication(users.findById(id).orElseThrow());
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> chooseOrganizer(auth, start));
            var second = executor.submit(() -> chooseOrganizer(auth, start));
            start.countDown();
            assertThat(new Integer[] {first.get(30, TimeUnit.SECONDS), second.get(30, TimeUnit.SECONDS)})
                    .containsExactlyInAnyOrder(200, 409);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM organizer_profiles WHERE user_id = ?",
                Integer.class, id)).isEqualTo(1);
    }

    private int chooseOrganizer(Authentication auth, CountDownLatch start) throws Exception {
        if (!start.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Test nije pokrenuo istodobne zahtjeve.");
        }
        return mockMvc.perform(post("/api/v1/me/role").with(authentication(auth)).with(withCsrf())
                        .contentType("application/json").content("{\"role\":\"ORGANIZER\"}"))
                .andReturn().getResponse().getStatus();
    }

    private UUID createOnboardingUser() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, email, display_name, status) VALUES (?, ?, ?, 'ONBOARDING')",
                id, id + "@artconnect.test", "Novi korisnik");
        return id;
    }
}
