package hr.fer.artconnect.opportunity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
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

        var response = mockMvc.perform(post(PATH)
                        .with(withCsrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(opportunityInput("Izložba za uređivanje", "Početni opis")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PREPARATION"))
                .andReturn()
                .getResponse();
        opportunityId = UUID.fromString(
                objectMapper.readTree(response.getContentAsString()).get("id").asString());
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
