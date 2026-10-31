package hr.fer.artconnect.opportunity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.auth.ArtConnectOidcUserService;
import hr.fer.artconnect.auth.CurrentUserProvider;
import hr.fer.artconnect.auth.SecurityConfig;
import hr.fer.artconnect.auth.domain.OrganizerProfileRepository;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRepository;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.catalog.domain.ArtFieldRepository;
import hr.fer.artconnect.catalog.domain.OpportunityType;
import hr.fer.artconnect.catalog.domain.OpportunityTypeRepository;
import hr.fer.artconnect.opportunity.domain.Location;
import hr.fer.artconnect.opportunity.domain.Opportunity;
import hr.fer.artconnect.opportunity.domain.OpportunityRepository;
import hr.fer.artconnect.opportunity.domain.OpportunityStatus;
import hr.fer.artconnect.opportunity.service.OpportunityDeletionGuard;
import hr.fer.artconnect.opportunity.service.OpportunityMapper;
import hr.fer.artconnect.opportunity.service.OpportunityService;
import hr.fer.artconnect.opportunity.web.OpportunityController;
import hr.fer.artconnect.support.TestPrincipals;
import hr.fer.artconnect.support.TestUsers;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = OpportunityController.class, properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-client",
        "spring.security.oauth2.client.registration.google.client-secret=test-secret"
})
@Import({SecurityConfig.class, CurrentUserProvider.class, OpportunityService.class, OpportunityMapper.class})
class OpportunityStatusEndpointTest {
    private static final UUID ID = UUID.randomUUID();
    private static final String PATH = "/api/v1/opportunities/" + ID + "/status";
    private static final String OPEN = "{\"status\":\"OPEN\"}";
    @Autowired MockMvc mvc;
    @MockitoBean ArtConnectOidcUserService oidcUserService;
    @MockitoBean UserRepository users;
    @MockitoBean OpportunityRepository opportunities;
    @MockitoBean OpportunityTypeRepository types;
    @MockitoBean ArtFieldRepository artFields;
    @MockitoBean OrganizerProfileRepository profiles;
    // @WebMvcTest ne skenira @Component, pa guard iz OpportunityServicea dolazi kao mock.
    @MockitoBean OpportunityDeletionGuard deletionGuard;
    @MockitoBean(name = "geocodingClock") Clock clock;
    private User organizer;
    private Opportunity opportunity;

    @BeforeEach
    void setup() {
        organizer = TestUsers.user(UUID.randomUUID(), "google-org", "org@test.hr",
                UserRole.ORGANIZER, UserStatus.ACTIVE);
        when(users.findById(organizer.getId())).thenReturn(Optional.of(organizer));
        var now = OffsetDateTime.parse("2026-10-30T10:00:00Z");
        when(clock.instant()).thenReturn(now.plusHours(1).toInstant());
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        opportunity = Opportunity.create(organizer, new OpportunityType(), Set.of(), "Izložba", "Opis",
                now.plusDays(1), null, null, Location.online("https://example.org"), null, null, now);
        when(opportunities.findById(ID)).thenReturn(Optional.of(opportunity));
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPEN", "CLOSED", "SELECTION"})
    void vlasnikMijenjaStatusIDobivaPriliku(String target) throws Exception {
        if (!target.equals("OPEN")) {
            ReflectionTestUtils.setField(opportunity, "status",
                    target.equals("CLOSED") ? OpportunityStatus.OPEN : OpportunityStatus.CLOSED);
        }
        when(opportunities.save(any())).thenAnswer(call -> call.getArgument(0));
        mvc.perform(post(PATH).with(authentication(TestPrincipals.authentication(organizer))).with(csrf())
                        .contentType("application/json").content("{\"status\":\"" + target + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(target))
                .andExpect(jsonPath("$.title").value("Izložba"))
                .andExpect(jsonPath("$.organizer.id").value(organizer.getId().toString()));

        assertThat(opportunity.getStatus().name()).isEqualTo(target);
        assertThat(opportunity.getUpdatedAt()).isAfter(opportunity.getCreatedAt());
        verify(opportunities).save(opportunity);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{}", "null", "{\"status\":null}", "{\"status\":\"\"}",
            "{\"status\":\"UNKNOWN\"}", "{\"status\":\"PREPARATION\"}", "{\"status\":\"FINISHED\"}",
            "{\"status\":0}"})
    void neispravnoTijeloVraca400PrijeDohvataPrilike(String body) throws Exception {
        mvc.perform(post(PATH).with(authentication(TestPrincipals.authentication(organizer))).with(csrf())
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        verify(opportunities, never()).findById(any());
    }

    @Test
    void nemaSesijeIliCsrfTokena() throws Exception {
        mvc.perform(post(PATH).with(csrf()).contentType("application/json").content(OPEN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        mvc.perform(post(PATH).with(authentication(TestPrincipals.authentication(organizer)))
                        .contentType("application/json").content(OPEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(opportunities, never()).findById(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ARTIST", "ADMIN"})
    void drugaUlogaNemaPristup(String role) throws Exception {
        ReflectionTestUtils.setField(organizer, "role", UserRole.valueOf(role));
        mvc.perform(post(PATH).with(authentication(TestPrincipals.authentication(organizer))).with(csrf())
                        .contentType("application/json").content(OPEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(opportunities, never()).findById(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"BLOCKED", "ONBOARDING"})
    void neaktivanOrganizatorNemaPristup(String userStatus) throws Exception {
        ReflectionTestUtils.setField(organizer, "status", UserStatus.valueOf(userStatus));
        mvc.perform(post(PATH).with(authentication(TestPrincipals.authentication(organizer))).with(csrf())
                        .contentType("application/json").content(OPEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value(
                        userStatus.equals("BLOCKED") ? "USER_BLOCKED" : "ONBOARDING_REQUIRED"));
        verify(opportunities, never()).findById(any());
    }

    @Test
    void nepostojecaPrilikaVraca404ATudja403() throws Exception {
        when(opportunities.findById(ID)).thenReturn(Optional.empty());
        mvc.perform(post(PATH).with(authentication(TestPrincipals.authentication(organizer))).with(csrf())
                        .contentType("application/json").content(OPEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));

        var other = TestUsers.user(UUID.randomUUID(), "google-other", "other@test.hr",
                UserRole.ORGANIZER, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(opportunity, "organizer", other);
        when(opportunities.findById(ID)).thenReturn(Optional.of(opportunity));
        mvc.perform(post(PATH).with(authentication(TestPrincipals.authentication(organizer))).with(csrf())
                        .contentType("application/json").content(OPEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(opportunities, never()).save(any());
    }
}
