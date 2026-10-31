package hr.fer.artconnect.opportunity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hr.fer.artconnect.auth.CurrentUserProvider;
import hr.fer.artconnect.auth.domain.OrganizerApproval;
import hr.fer.artconnect.auth.domain.OrganizerProfileRepository;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.catalog.domain.ArtField;
import hr.fer.artconnect.catalog.domain.ArtFieldRepository;
import hr.fer.artconnect.catalog.domain.OpportunityType;
import hr.fer.artconnect.catalog.domain.OpportunityTypeRepository;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import hr.fer.artconnect.opportunity.domain.Location;
import hr.fer.artconnect.opportunity.domain.LocationType;
import hr.fer.artconnect.opportunity.domain.Opportunity;
import hr.fer.artconnect.opportunity.domain.OpportunityRepository;
import hr.fer.artconnect.opportunity.domain.OpportunityStatus;
import hr.fer.artconnect.opportunity.service.OpportunityDeletionGuard;
import hr.fer.artconnect.opportunity.service.OpportunityMapper;
import hr.fer.artconnect.opportunity.service.OpportunityService;
import hr.fer.artconnect.opportunity.web.LocationInput;
import hr.fer.artconnect.opportunity.web.OpportunityInput;
import hr.fer.artconnect.support.TestUsers;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

class OpportunityServiceTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-26T10:00:00Z");
    private static final UUID TYPE_ID = UUID.randomUUID();
    private static final UUID FIELD_ID = UUID.randomUUID();
    private static final UUID OPPORTUNITY_ID = UUID.randomUUID();
    private final OpportunityRepository opportunities = mock(OpportunityRepository.class);
    private final OpportunityTypeRepository types = mock(OpportunityTypeRepository.class);
    private final ArtFieldRepository fields = mock(ArtFieldRepository.class);
    private final OrganizerProfileRepository profiles = mock(OrganizerProfileRepository.class);
    private final CurrentUserProvider currentUser = mock(CurrentUserProvider.class);
    private final OpportunityDeletionGuard deletionGuard = mock(OpportunityDeletionGuard.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-26T10:00:00Z"), ZoneOffset.UTC);
    private final OpportunityService service = new OpportunityService(
            opportunities, types, fields, profiles, currentUser, new OpportunityMapper(), deletionGuard, clock);

    @BeforeEach
    void organizer() {
        var user = TestUsers.user(UUID.randomUUID(), "google", "org@test.hr", UserRole.ORGANIZER, UserStatus.ACTIVE);
        ReflectionTestUtils.setField(user, "organizerApproval", OrganizerApproval.PENDING);
        when(currentUser.getCurrentUser()).thenReturn(user);
    }

    @Test
    void spremaNacrtOrganizatoraNaCekanjuPremaFiksnomSatu() {
        activeCatalogs();
        var input = request(NOW.plusNanos(1), NOW.plusDays(2), NOW.plusDays(2), physical(), List.of(FIELD_ID));

        var result = service.create(input);

        assertThat(result.status()).isEqualTo(OpportunityStatus.PREPARATION);
        assertThat(result.organizer().id()).isEqualTo(currentUser.getCurrentUser().getId());
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(result.updatedAt()).isEqualTo(NOW);
        assertThat(result.publishedAt()).isNull();
        assertThat(result.location().latitude()).isEqualByComparingTo("45.123457");
        assertThat(result.location().longitude()).isEqualByComparingTo("16.123457");
        assertThat(result.location().onlineUrl()).isNull();
        assertThat(result.title()).isEqualTo("Naslov");
        assertThat(result.description()).isEqualTo("Opis");
        assertThat(result.criteria()).isNull();
        assertThat(result.prize()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://example.org", "https://example.org/event", "HTTPS://example.org"})
    void spremaOnlinePrilikuBezDatumaDogadjanjaIFizickihPolja(String url) {
        activeCatalogs();
        var input = request(NOW.plusDays(1), null, null,
                new LocationInput(LocationType.ONLINE, "Adresa", "Grad", "10000",
                        BigDecimal.TEN, BigDecimal.TEN, " " + url + " "), List.of(FIELD_ID));

        var result = service.create(input);

        assertThat(result.location().onlineUrl()).isEqualTo(url);
        assertThat(result.location().streetAddress()).isNull();
        assertThat(result.location().latitude()).isNull();
        assertThat(result.eventStart()).isNull();
        assertThat(result.eventEnd()).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void odbijaProsliRokIRokJednakSadasnjemTrenutku(int seconds) {
        invalid(request(NOW.plusSeconds(seconds).withOffsetSameInstant(ZoneOffset.ofHours(2)),
                null, null, physical(), List.of(FIELD_ID)), "deadline");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void odbijaKrajBezPocetkaIKrajPrijePocetka(boolean missingStart) {
        invalid(request(NOW.plusDays(1), missingStart ? null : NOW.plusDays(3), NOW.plusDays(2),
                physical(), List.of(FIELD_ID)), "eventEnd");
    }

    @Test
    void datumeUsporedjujeKaoTrenutkeNeKaoLokalnoVrijeme() {
        activeCatalogs();
        var result = service.create(request(NOW.plusDays(1),
                OffsetDateTime.parse("2026-11-01T14:00:00+02:00"),
                OffsetDateTime.parse("2026-11-01T13:00:00Z"), physical(), List.of(FIELD_ID)));
        assertThat(result.eventEnd().isAfter(result.eventStart())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"streetAddress", "city", "latitude", "longitude"})
    void odbijaNedostajuceObaveznoPoljeFizickeLokacije(String field) {
        var location = new LocationInput(LocationType.PHYSICAL,
                field.equals("streetAddress") ? "  " : "Adresa", field.equals("city") ? "  " : "Grad", null,
                field.equals("latitude") ? null : BigDecimal.TEN,
                field.equals("longitude") ? null : BigDecimal.TEN, null);
        invalid(request(NOW.plusDays(1), null, null, location, List.of(FIELD_ID)), "location." + field);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "ftp://example.org", "javascript:alert(1)", "/event", "https:example.org",
            "https://", "https:///event", "https://example.org/a b"})
    void odbijaNedostajucuINevaljanuOnlinePoveznicu(String url) {
        invalid(request(NOW.plusDays(1), null, null,
                new LocationInput(LocationType.ONLINE, null, null, null, null, null, url),
                List.of(FIELD_ID)), "location.onlineUrl");
    }

    @Test
    void odbijaDuplikatePodrucjaUmjestoTihogUklanjanja() {
        invalid(request(NOW.plusDays(1), null, null, physical(), List.of(FIELD_ID, FIELD_ID)), "artFieldIds");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void odbijaNepostojecuINeaktivnuVrstu(boolean exists) {
        var type = new OpportunityType();
        type.update("exhibition", "Izložba", false, (short) 0);
        when(types.findById(TYPE_ID)).thenReturn(exists ? Optional.of(type) : Optional.empty());
        invalid(request(NOW.plusDays(1), null, null, physical(), List.of(FIELD_ID)), "typeId");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void odbijaNepostojeceINeaktivnoPodrucje(boolean exists) {
        activeType();
        var field = new ArtField();
        field.update("painting", "Slikarstvo", false, (short) 0);
        when(fields.findAllById(List.of(FIELD_ID))).thenReturn(exists ? List.of(field) : List.of());
        invalid(request(NOW.plusDays(1), null, null, physical(), List.of(FIELD_ID)), "artFieldIds");
    }

    @Test
    void blokiraniOrganizatorNeMozeStvaratiPrilike() {
        ReflectionTestUtils.setField(currentUser.getCurrentUser(), "status", UserStatus.BLOCKED);
        assertThatThrownBy(() -> service.create(request(NOW.plusDays(1), null, null, physical(), List.of(FIELD_ID))))
                .isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.getCode()).isEqualTo(ErrorCode.USER_BLOCKED));
        verify(opportunities, never()).save(any());
    }

    @Test
    void vlasnikBriseSvojuPrilikuNakonProvjereGuarda() {
        var opportunity = opportunityOf(currentUser.getCurrentUser());
        when(opportunities.findById(OPPORTUNITY_ID)).thenReturn(Optional.of(opportunity));

        service.delete(OPPORTUNITY_ID);

        verify(deletionGuard).ensureDeletable(opportunity);
        verify(opportunities).delete(opportunity);
    }

    @Test
    void brisanjeNepostojecePrilikeVraca404() {
        when(opportunities.findById(OPPORTUNITY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(OPPORTUNITY_ID)).isInstanceOfSatisfying(ApiException.class,
                error -> assertThat(error.getCode()).isEqualTo(ErrorCode.NOT_FOUND));
        verify(opportunities, never()).delete(any());
    }

    @Test
    void brisanjeTudjePrilikeVraca403IneTrosiGuard() {
        var other = TestUsers.user(UUID.randomUUID(), "google", "drugi@test.hr", UserRole.ORGANIZER,
                UserStatus.ACTIVE);
        when(opportunities.findById(OPPORTUNITY_ID)).thenReturn(Optional.of(opportunityOf(other)));

        assertThatThrownBy(() -> service.delete(OPPORTUNITY_ID)).isInstanceOfSatisfying(ApiException.class,
                error -> assertThat(error.getCode()).isEqualTo(ErrorCode.FORBIDDEN));
        // Vlasništvo se provjerava prije guarda, pa tuđa prilika ne otkriva ima li prijava.
        verify(deletionGuard, never()).ensureDeletable(any());
        verify(opportunities, never()).delete(any());
    }

    @Test
    void guardOdbijaBrisanjeIPrilikaOstaje() {
        var opportunity = opportunityOf(currentUser.getCurrentUser());
        when(opportunities.findById(OPPORTUNITY_ID)).thenReturn(Optional.of(opportunity));
        doThrow(new ApiException(ErrorCode.HAS_APPLICATIONS, "Prilika ima prijave."))
                .when(deletionGuard).ensureDeletable(opportunity);

        assertThatThrownBy(() -> service.delete(OPPORTUNITY_ID)).isInstanceOfSatisfying(ApiException.class,
                error -> assertThat(error.getCode()).isEqualTo(ErrorCode.HAS_APPLICATIONS));
        verify(opportunities, never()).delete(any());
    }

    @ParameterizedTest
    @CsvSource({"OPEN,CLOSED", "PREPARATION,PREPARATION", "CLOSED,CLOSED", "SELECTION,SELECTION",
            "FINISHED,FINISHED"})
    void arhiviranjeZatvaraSamoOtvorenuPriliku(OpportunityStatus current, OpportunityStatus expected) {
        var opportunity = opportunityOf(currentUser.getCurrentUser());
        ReflectionTestUtils.setField(opportunity, "status", current);
        when(opportunities.findById(OPPORTUNITY_ID)).thenReturn(Optional.of(opportunity));

        var result = service.archive(OPPORTUNITY_ID);

        assertThat(result.archived()).isTrue();
        assertThat(result.status()).isEqualTo(expected);
        assertThat(result.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void ponovnoArhiviranjeNeMijenjaPrilikuNiVrijemeIzmjene() {
        var opportunity = opportunityOf(currentUser.getCurrentUser());
        ReflectionTestUtils.setField(opportunity, "status", OpportunityStatus.SELECTION);
        ReflectionTestUtils.setField(opportunity, "archived", true);
        ReflectionTestUtils.setField(opportunity, "updatedAt", NOW.minusDays(5));
        when(opportunities.findById(OPPORTUNITY_ID)).thenReturn(Optional.of(opportunity));

        var result = service.archive(OPPORTUNITY_ID);

        assertThat(result.archived()).isTrue();
        assertThat(result.status()).isEqualTo(OpportunityStatus.SELECTION);
        assertThat(result.updatedAt()).isEqualTo(NOW.minusDays(5));
    }

    @Test
    void arhiviranjeTudjePrilikeVraca403() {
        var other = TestUsers.user(UUID.randomUUID(), "google", "drugi@test.hr", UserRole.ORGANIZER,
                UserStatus.ACTIVE);
        var opportunity = opportunityOf(other);
        when(opportunities.findById(OPPORTUNITY_ID)).thenReturn(Optional.of(opportunity));

        assertThatThrownBy(() -> service.archive(OPPORTUNITY_ID)).isInstanceOfSatisfying(ApiException.class,
                error -> assertThat(error.getCode()).isEqualTo(ErrorCode.FORBIDDEN));
        assertThat(opportunity.isArchived()).isFalse();
    }

    private void invalid(OpportunityInput input, String field) {
        assertThatThrownBy(() -> service.create(input)).isInstanceOfSatisfying(ApiException.class, error -> {
            assertThat(error.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
            assertThat(error.getDetails()).extracting(issue -> issue.field()).containsExactly(field);
        });
        verify(opportunities, never()).save(any());
    }

    private void activeType() {
        var type = new OpportunityType();
        type.update("exhibition", "Izložba", true, (short) 0);
        ReflectionTestUtils.setField(type, "id", TYPE_ID);
        when(types.findById(TYPE_ID)).thenReturn(Optional.of(type));
    }

    private void activeCatalogs() {
        activeType();
        var field = new ArtField();
        field.update("painting", "Slikarstvo", true, (short) 0);
        ReflectionTestUtils.setField(field, "id", FIELD_ID);
        when(fields.findAllById(List.of(FIELD_ID))).thenReturn(List.of(field));
        when(opportunities.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    private Opportunity opportunityOf(User owner) {
        var type = new OpportunityType();
        type.update("exhibition", "Izložba", true, (short) 0);
        return Opportunity.create(owner, type, Set.of(), "Naslov", "Opis", NOW.plusDays(1), null, null,
                Location.online("https://example.org"), null, null, NOW);
    }

    private LocationInput physical() {
        return new LocationInput(LocationType.PHYSICAL, " Adresa ", " Grad ", "10000",
                new BigDecimal("45.1234567"), new BigDecimal("16.1234567"), "https://ignored.org");
    }

    private OpportunityInput request(OffsetDateTime deadline, OffsetDateTime start, OffsetDateTime end,
            LocationInput location, List<UUID> ids) {
        return new OpportunityInput(" Naslov ", " Opis ", TYPE_ID, ids, deadline, " ", " ", location, start, end);
    }
}
