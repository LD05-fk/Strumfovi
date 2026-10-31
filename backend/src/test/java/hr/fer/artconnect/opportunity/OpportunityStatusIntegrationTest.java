package hr.fer.artconnect.opportunity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.support.IntegrationTestBase;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

class OpportunityStatusIntegrationTest extends IntegrationTestBase {
    private static final Instant NOW = Instant.parse("2026-10-30T10:00:00Z");
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @MockitoBean(name = "geocodingClock") Clock clock;
    private UUID id;
    private UUID owner;
    private String path;

    @BeforeEach
    void createDraft() throws Exception {
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        owner = loginAs("ORGANIZER");
        var type = jdbc.queryForObject("SELECT id FROM opportunity_types WHERE active = true LIMIT 1", UUID.class);
        var field = jdbc.queryForObject("SELECT id FROM art_fields WHERE active = true LIMIT 1", UUID.class);
        var response = mockMvc.perform(post("/api/v1/opportunities").with(withCsrf())
                        .contentType("application/json").content("""
                                {"title":"Izložba", "description":"Opis izložbe", "typeId":"%s",
                                 "artFieldIds":["%s"], "deadline":"2026-11-30T10:00:00Z",
                                 "location":{"type":"ONLINE", "onlineUrl":"https://example.org"}}
                                """.formatted(type, field)))
                .andExpect(status().isCreated()).andReturn().getResponse();
        id = UUID.fromString(mapper.readTree(response.getContentAsString()).get("id").asString());
        path = "/api/v1/opportunities/" + id + "/status";
    }

    @Test
    void objavaZatvaranjePonovnoOtvaranjeIOdabirCuvajuPrvuObjavu() throws Exception {
        // Privremeno odobravanje svih organizatora vrijedi i za postojeći PENDING račun.
        jdbc.update("UPDATE users SET organizer_approval = 'PENDING' WHERE id = ?", owner);
        var before = jdbc.queryForMap("SELECT * FROM opportunities WHERE id = ?", id);
        before.keySet().removeAll(Set.of("status", "updated_at", "published_at"));
        Instant firstPublication = NOW.plusSeconds(60);
        String[] targets = {"OPEN", "CLOSED", "OPEN", "CLOSED", "SELECTION"};
        for (int i = 0; i < targets.length; i++) {
            var time = firstPublication.plusSeconds(i * 60L);
            when(clock.instant()).thenReturn(time);
            send(targets[i]).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id.toString()))
                    .andExpect(jsonPath("$.status").value(targets[i]))
                    .andExpect(jsonPath("$.title").value("Izložba"))
                    .andExpect(jsonPath("$.organizer.id").value(owner.toString()))
                    .andExpect(jsonPath("$.publishedAt").value(firstPublication.toString()))
                    .andExpect(jsonPath("$.updatedAt").value(time.toString()));
            var row = jdbc.queryForMap("SELECT * FROM opportunities WHERE id = ?", id);
            assertThat(row.get("status")).isEqualTo(targets[i]);
            assertThat(((Timestamp) row.get("published_at")).toInstant()).isEqualTo(firstPublication);
            assertThat(((Timestamp) row.get("updated_at")).toInstant()).isEqualTo(time);
            row.keySet().removeAll(Set.of("status", "updated_at", "published_at"));
            assertThat(row).isEqualTo(before);
        }
    }

    @Test
    void odbijeniPrijelaziNeMijenjajuBazu() throws Exception {
        var before = jdbc.queryForMap("SELECT * FROM opportunities WHERE id = ?", id);
        send("CLOSED").andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATUS_TRANSITION"));
        assertThat(jdbc.queryForMap("SELECT * FROM opportunities WHERE id = ?", id)).isEqualTo(before);

        for (String current : new String[] {"PREPARATION", "CLOSED"}) {
            jdbc.update("UPDATE opportunities SET status = ?, deadline = ? WHERE id = ?",
                    current, OffsetDateTime.ofInstant(NOW, ZoneOffset.UTC), id);
            before = jdbc.queryForMap("SELECT * FROM opportunities WHERE id = ?", id);
            send("OPEN").andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("DEADLINE_PASSED"));
            assertThat(jdbc.queryForMap("SELECT * FROM opportunities WHERE id = ?", id)).isEqualTo(before);
        }
        jdbc.update("UPDATE opportunities SET archived = true WHERE id = ?", id);
        before = jdbc.queryForMap("SELECT * FROM opportunities WHERE id = ?", id);
        send("SELECTION").andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATUS_TRANSITION"));
        assertThat(jdbc.queryForMap("SELECT * FROM opportunities WHERE id = ?", id)).isEqualTo(before);
    }

    @Test
    void neispravanUnosNemaSesijeIPogresanVlasnik() throws Exception {
        send("FINISHED").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        mockMvc.perform(post(path).with(anonymous()).with(withCsrf())
                        .contentType("application/json").content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        mockMvc.perform(post("/api/v1/opportunities/" + UUID.randomUUID() + "/status").with(withCsrf())
                        .contentType("application/json").content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        loginAs("ORGANIZER");
        send("OPEN").andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        assertThat(jdbc.queryForObject("SELECT status FROM opportunities WHERE id = ?", String.class, id))
                .isEqualTo("PREPARATION");
    }

    private ResultActions send(String target) throws Exception {
        return mockMvc.perform(post(path).with(withCsrf()).contentType("application/json")
                .content("{\"status\":\"" + target + "\"}"));
    }
}
