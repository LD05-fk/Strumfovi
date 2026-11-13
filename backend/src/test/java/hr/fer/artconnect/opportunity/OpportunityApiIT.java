package hr.fer.artconnect.opportunity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.support.IntegrationTestBase;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;

class OpportunityApiIT extends IntegrationTestBase {
    private static final String PATH = "/api/v1/opportunities";
    private static final Instant NOW = Instant.parse("2026-10-26T10:00:00Z");

    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper objectMapper;
    @MockitoBean(name = "geocodingClock") Clock clock;

    private UUID ownerId;
    private UUID opportunityId;
    private UUID typeId;
    private UUID artFieldId;

    @BeforeEach
    void createOpportunityOwnedByOrganizer() throws Exception {
        when(clock.instant()).thenReturn(NOW);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);

        ownerId = loginAs("ORGANIZER");
        typeId = jdbc.queryForObject(
                "SELECT id FROM opportunity_types WHERE active = true LIMIT 1", UUID.class);
        artFieldId = jdbc.queryForObject(
                "SELECT id FROM art_fields WHERE active = true LIMIT 1", UUID.class);

        opportunityId = createOpportunity("Izložba za uređivanje", "Početni opis");
    }

    @Test
    void drugiOrganizatorNeMozeUreditiTuduPriliku() throws Exception {
        loginAs("ORGANIZER");

        mockMvc.perform(put(PATH + "/" + opportunityId)
                        .with(withCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(opportunityInput("Neovlaštena izmjena", "Izmijenjen opis")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        assertThat(jdbc.queryForObject(
                "SELECT title FROM opportunities WHERE id = ?", String.class, opportunityId))
                .isEqualTo("Izložba za uređivanje");
        assertThat(jdbc.queryForObject(
                "SELECT description FROM opportunities WHERE id = ?", String.class, opportunityId))
                .isEqualTo("Početni opis");
        assertThat(jdbc.queryForObject(
                "SELECT organizer_id FROM opportunities WHERE id = ?", UUID.class, opportunityId))
                .isEqualTo(ownerId);
    }

    @Test
    void vlasnikMozeUreditiSvojuPriliku() throws Exception {
        mockMvc.perform(put(PATH + "/" + opportunityId)
                        .with(withCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(opportunityInput("Uređena izložba", "Ažurirani opis")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(opportunityId.toString()))
                .andExpect(jsonPath("$.title").value("Uređena izložba"))
                .andExpect(jsonPath("$.description").value("Ažurirani opis"))
                .andExpect(jsonPath("$.organizer.id").value(ownerId.toString()))
                .andExpect(jsonPath("$.status").value("PREPARATION"));

        assertThat(jdbc.queryForObject(
                "SELECT title FROM opportunities WHERE id = ?", String.class, opportunityId))
                .isEqualTo("Uređena izložba");
        assertThat(jdbc.queryForObject(
                "SELECT description FROM opportunities WHERE id = ?", String.class, opportunityId))
                .isEqualTo("Ažurirani opis");
        assertThat(jdbc.queryForObject(
                "SELECT organizer_id FROM opportunities WHERE id = ?", UUID.class, opportunityId))
                .isEqualTo(ownerId);
    }

    // OPP-03-TEST: puni životni ciklus prilike preko krajnje točke statusa i javne pretrage.
    @Test
    void vlasnikObjavljujeZatvaraPonovnoOtvaraIZapocinjeOdabir() throws Exception {
        String[] targets = {"OPEN", "CLOSED", "OPEN", "CLOSED", "SELECTION"};
        for (int i = 0; i < targets.length; i++) {
            when(clock.instant()).thenReturn(NOW.plusSeconds(60L * (i + 1)));
            changeStatus(opportunityId, targets[i])
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(opportunityId.toString()))
                    .andExpect(jsonPath("$.status").value(targets[i]));
        }

        mockMvc.perform(get(PATH).param("statuses", "SELECTION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(opportunityId.toString()))
                .andExpect(jsonPath("$.data[0].status").value("SELECTION"));
    }

    @Test
    void zatvorenaPrilikaSIsteklimRokomSeNeMozePonovnoOtvoriti() throws Exception {
        changeStatus(opportunityId, "OPEN").andExpect(status().isOk());
        changeStatus(opportunityId, "CLOSED").andExpect(status().isOk());

        // Rok iz opportunityInput je 2026-11-30T10:00:00Z, pa je ovaj trenutak nakon njega.
        when(clock.instant()).thenReturn(Instant.parse("2026-12-01T10:00:00Z"));
        changeStatus(opportunityId, "OPEN")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DEADLINE_PASSED"));

        assertThat(statusOf(opportunityId)).isEqualTo("CLOSED");
    }

    @Test
    void prilikaUOdabiruNeMozeNatragUOtvorenoIliZatvoreno() throws Exception {
        for (String target : new String[] {"OPEN", "CLOSED", "SELECTION"}) {
            changeStatus(opportunityId, target).andExpect(status().isOk());
        }

        for (String target : new String[] {"OPEN", "CLOSED"}) {
            changeStatus(opportunityId, target)
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error.code").value("INVALID_STATUS_TRANSITION"));
            assertThat(statusOf(opportunityId)).isEqualTo("SELECTION");
        }
    }

    @Test
    void drugiOrganizatorNeMozeArhiviratiObrisatiNiMijenjatiStatusTudePrilike() throws Exception {
        changeStatus(opportunityId, "OPEN").andExpect(status().isOk());
        loginAs("ORGANIZER");

        mockMvc.perform(post(PATH + "/" + opportunityId + "/archive").with(withCsrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        mockMvc.perform(delete(PATH + "/" + opportunityId).with(withCsrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        changeStatus(opportunityId, "CLOSED")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        assertThat(jdbc.queryForMap(
                "SELECT status, archived, organizer_id FROM opportunities WHERE id = ?", opportunityId))
                .containsEntry("status", "OPEN")
                .containsEntry("archived", false)
                .containsEntry("organizer_id", ownerId);
    }

    @Test
    void arhiviranaPrilikaJeSamoNaPopisuArhiviranihMojihPrilika() throws Exception {
        UUID activeId = createOpportunity("Aktivna izložba", "Ostaje na aktivnom popisu");
        changeStatus(opportunityId, "OPEN").andExpect(status().isOk());

        mockMvc.perform(post(PATH + "/" + opportunityId + "/archive").with(withCsrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(get("/api/v1/me/opportunities").param("archived", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(opportunityId.toString()))
                .andExpect(jsonPath("$.data[0].archived").value(true))
                .andExpect(jsonPath("$.data[0].status").value("CLOSED"));
        mockMvc.perform(get("/api/v1/me/opportunities").param("archived", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(activeId.toString()));
    }

    @Test
    void vlasnikBriseSvojuPrilikuBezPrijava() throws Exception {
        mockMvc.perform(delete(PATH + "/" + opportunityId).with(withCsrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(PATH + "/" + opportunityId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        mockMvc.perform(get("/api/v1/me/opportunities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    private UUID createOpportunity(String title, String description) throws Exception {
        var response = mockMvc.perform(post(PATH)
                        .with(withCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(opportunityInput(title, description)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PREPARATION"))
                .andReturn()
                .getResponse();
        return UUID.fromString(
                objectMapper.readTree(response.getContentAsString()).get("id").asString());
    }

    private ResultActions changeStatus(UUID id, String target) throws Exception {
        return mockMvc.perform(post(PATH + "/" + id + "/status")
                .with(withCsrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + target + "\"}"));
    }

    private String statusOf(UUID id) {
        return jdbc.queryForObject("SELECT status FROM opportunities WHERE id = ?", String.class, id);
    }

    private String opportunityInput(String title, String description) {
        return """
                {
                  "title": "%s",
                  "description": "%s",
                  "typeId": "%s",
                  "artFieldIds": ["%s"],
                  "deadline": "2026-11-30T10:00:00Z",
                  "location": {
                    "type": "ONLINE",
                    "onlineUrl": "https://example.org/event"
                  }
                }
                """.formatted(title, description, typeId, artFieldId);
    }
}
