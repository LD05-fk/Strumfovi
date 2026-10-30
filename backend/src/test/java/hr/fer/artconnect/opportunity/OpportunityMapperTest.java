package hr.fer.artconnect.opportunity;

import static org.assertj.core.api.Assertions.assertThat;

import hr.fer.artconnect.auth.domain.OrganizerProfile;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.catalog.domain.ArtField;
import hr.fer.artconnect.catalog.domain.OpportunityType;
import hr.fer.artconnect.opportunity.domain.Location;
import hr.fer.artconnect.opportunity.domain.LocationType;
import hr.fer.artconnect.opportunity.domain.Opportunity;
import hr.fer.artconnect.opportunity.domain.OpportunityStatus;
import hr.fer.artconnect.opportunity.service.OpportunityMapper;
import hr.fer.artconnect.opportunity.service.ZeroApplicationCountsProvider;
import hr.fer.artconnect.opportunity.web.ApplicationCounts;
import hr.fer.artconnect.opportunity.web.CatalogRef;
import hr.fer.artconnect.support.TestUsers;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class OpportunityMapperTest {
    private final OpportunityMapper mapper = new OpportunityMapper();

    @Test
    void mapiraFizickiNacrtIPodatkeOrganizatoraUPuniISazetiOdgovor() {
        var user = TestUsers.user(UUID.randomUUID(), "google", "org@test.hr", UserRole.ORGANIZER, UserStatus.ACTIVE);
        var profile = OrganizerProfile.createFor(user);
        ReflectionTestUtils.setField(profile, "organizationName", "Galerija");
        var type = new OpportunityType();
        type.update("exhibition", "Izložba", true, (short) 0);
        ReflectionTestUtils.setField(type, "id", UUID.randomUUID());
        var painting = field("painting", "Slikarstvo", 2);
        var sculpture = field("sculpture", "Kiparstvo", 1);
        var fields = new LinkedHashSet<>(Set.of(painting, sculpture));
        var location = Location.physical("Ilica 1", "Zagreb", "10000",
                new BigDecimal("45.1234567"), new BigDecimal("-16.1234565"));
        var now = OffsetDateTime.parse("2026-10-26T10:00:00Z");
        var opportunity = Opportunity.create(user, type, fields, "Izložba", "Opis", now.plusDays(10),
                "Kriteriji", "Nagrada", location, now.plusDays(20), now.plusDays(21), now);
        UUID id = UUID.randomUUID();
        ReflectionTestUtils.setField(opportunity, "id", id);
        fields.clear();

        var dto = mapper.toDto(opportunity, profile);
        var summary = mapper.toSummary(opportunity, profile);

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.title()).isEqualTo("Izložba");
        assertThat(dto.description()).isEqualTo("Opis");
        assertThat(dto.type()).isEqualTo(new CatalogRef(type.getId(), "exhibition", "Izložba"));
        assertThat(dto.artFields()).containsExactly(
                new CatalogRef(sculpture.getId(), "sculpture", "Kiparstvo"),
                new CatalogRef(painting.getId(), "painting", "Slikarstvo"));
        assertThat(dto.deadline()).isEqualTo(now.plusDays(10));
        assertThat(dto.criteria()).isEqualTo("Kriteriji");
        assertThat(dto.prize()).isEqualTo("Nagrada");
        assertThat(dto.eventStart()).isEqualTo(now.plusDays(20));
        assertThat(dto.eventEnd()).isEqualTo(now.plusDays(21));
        assertThat(dto.status()).isEqualTo(OpportunityStatus.PREPARATION);
        assertThat(dto.archived()).isFalse();
        assertThat(dto.hiddenByAdmin()).isFalse();
        assertThat(dto.publishedAt()).isNull();
        assertThat(dto.createdAt()).isEqualTo(now);
        assertThat(dto.updatedAt()).isEqualTo(now);
        assertThat(dto.hasResults()).isFalse();
        assertThat(dto.myApplicationId()).isNull();
        assertThat(dto.organizer().id()).isEqualTo(user.getId());
        assertThat(dto.organizer().displayName()).isEqualTo(user.getDisplayName());
        assertThat(dto.organizer().organizationName()).isEqualTo("Galerija");
        assertThat(dto.location().type()).isEqualTo(LocationType.PHYSICAL);
        assertThat(dto.location().streetAddress()).isEqualTo("Ilica 1");
        assertThat(dto.location().city()).isEqualTo("Zagreb");
        assertThat(dto.location().postalCode()).isEqualTo("10000");
        assertThat(dto.location().latitude()).isEqualByComparingTo("45.123457");
        assertThat(dto.location().longitude()).isEqualByComparingTo("-16.123457");
        assertThat(dto.location().onlineUrl()).isNull();
        assertThat(summary.id()).isEqualTo(dto.id());
        assertThat(summary.title()).isEqualTo(dto.title());
        assertThat(summary.type()).isEqualTo(dto.type());
        assertThat(summary.artFields()).isEqualTo(dto.artFields());
        assertThat(summary.deadline()).isEqualTo(dto.deadline());
        assertThat(summary.status()).isEqualTo(dto.status());
        assertThat(summary.organizer()).isEqualTo(dto.organizer());
        assertThat(summary.eventStart()).isEqualTo(dto.eventStart());
        assertThat(summary.locationType()).isEqualTo(dto.location().type());
        assertThat(summary.city()).isEqualTo(dto.location().city());
        assertThat(summary.latitude()).isEqualTo(dto.location().latitude());
        assertThat(summary.longitude()).isEqualTo(dto.location().longitude());
        assertThat(opportunity.getHiddenAt()).isNull();
        assertThat(opportunity.getArtFields()).hasSize(2);
    }

    @Test
    void mapiraOnlineLokacijuBezFizickihPoljaINeobaveznogProfila() {
        var user = TestUsers.user(UUID.randomUUID(), "google", "org@test.hr", UserRole.ORGANIZER, UserStatus.ACTIVE);
        var type = new OpportunityType();
        type.update("workshop", "Radionica", true, (short) 0);
        var now = OffsetDateTime.parse("2026-10-26T10:00:00Z");
        var opportunity = Opportunity.create(user, type, Set.of(field("painting", "Slikarstvo", 0)),
                "Radionica", "Opis", now.plusDays(1), null, null,
                Location.online("https://example.org"), null, null, now);

        var dto = mapper.toDto(opportunity, null);
        var summary = mapper.toSummary(opportunity, null);

        assertThat(dto.location().type()).isEqualTo(LocationType.ONLINE);
        assertThat(dto.location().onlineUrl()).isEqualTo("https://example.org");
        assertThat(dto.location().streetAddress()).isNull();
        assertThat(dto.location().city()).isNull();
        assertThat(dto.location().postalCode()).isNull();
        assertThat(dto.location().latitude()).isNull();
        assertThat(dto.location().longitude()).isNull();
        assertThat(dto.organizer().organizationName()).isNull();
        assertThat(dto.criteria()).isNull();
        assertThat(dto.prize()).isNull();
        assertThat(dto.eventStart()).isNull();
        assertThat(dto.eventEnd()).isNull();
        assertThat(summary.locationType()).isEqualTo(LocationType.ONLINE);
        assertThat(summary.city()).isNull();
        assertThat(summary.latitude()).isNull();
        assertThat(summary.longitude()).isNull();
    }

    @Test
    void mapiraBrojacePrijavaKaoNule() {
        var user = TestUsers.user(UUID.randomUUID(), "google", "org@test.hr", UserRole.ORGANIZER, UserStatus.ACTIVE);
        var type = new OpportunityType();
        type.update("workshop", "Radionica", true, (short) 0);
        var now = OffsetDateTime.parse("2026-10-26T10:00:00Z");
        var opportunity = Opportunity.create(user, type, Set.of(field("painting", "Slikarstvo", 0)),
                "Radionica", "Opis", now.plusDays(1), null, null,
                Location.online("https://example.org"), null, null, now);
        ReflectionTestUtils.setField(opportunity, "id", UUID.randomUUID());

        var counts = new ZeroApplicationCountsProvider().getCounts(opportunity.getId());
        var result = mapper.toOrganizerOpportunity(opportunity, null, counts);

        assertThat(result.applicationCounts()).isEqualTo(new ApplicationCounts(0, 0, 0, 0, 0, 0));
    }

    private ArtField field(String code, String name, int position) {
        ArtField field = new ArtField();
        field.update(code, name, true, (short) position);
        ReflectionTestUtils.setField(field, "id", UUID.randomUUID());
        return field;
    }
}
