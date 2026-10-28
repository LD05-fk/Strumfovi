package hr.fer.artconnect.opportunity;

import static org.assertj.core.api.Assertions.assertThat;

import hr.fer.artconnect.auth.domain.OrganizerProfileRepository;
import hr.fer.artconnect.auth.domain.UserRepository;
import hr.fer.artconnect.catalog.domain.ArtFieldRepository;
import hr.fer.artconnect.catalog.domain.OpportunityTypeRepository;
import hr.fer.artconnect.opportunity.domain.Location;
import hr.fer.artconnect.opportunity.domain.LocationType;
import hr.fer.artconnect.opportunity.domain.Opportunity;
import hr.fer.artconnect.opportunity.domain.OpportunityRepository;
import hr.fer.artconnect.opportunity.domain.OpportunityStatus;
import hr.fer.artconnect.opportunity.service.OpportunityMapper;
import hr.fer.artconnect.support.IntegrationTestBase;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

class OpportunityPersistenceIntegrationTest extends IntegrationTestBase {
    @Autowired OpportunityRepository opportunities;
    @Autowired UserRepository users;
    @Autowired OpportunityTypeRepository types;
    @Autowired ArtFieldRepository fields;
    @Autowired OrganizerProfileRepository profiles;
    @Autowired OpportunityMapper mapper;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper objectMapper;

    @ParameterizedTest
    @EnumSource(LocationType.class)
    @Transactional
    void spremaIUcitavaNacrtPremaPostojecojShemi(LocationType locationType) {
        UUID organizerId = loginAs("ORGANIZER");
        jdbc.update("INSERT INTO organizer_profiles (user_id, organization_name) VALUES (?, ?)",
                organizerId, "Galerija");
        var type = types.findAll().getFirst();
        var artField = fields.findAll().getFirst();
        var now = OffsetDateTime.parse("2026-10-26T10:00:00Z");
        Location location = locationType == LocationType.PHYSICAL
                ? Location.physical("Ilica 1", "Zagreb", "10000", new BigDecimal("45.1234567"),
                        new BigDecimal("16.1234567"))
                : Location.online("https://example.org/event");
        var opportunity = Opportunity.create(users.findById(organizerId).orElseThrow(), type,
                Set.of(artField), "Izložba", "Opis izložbe", now.plusDays(10), "Kriteriji", "Nagrada",
                location, now.plusDays(20), now.plusDays(21), now);

        UUID id = opportunities.saveAndFlush(opportunity).getId();
        entityManager.clear();
        var loaded = opportunities.findById(id).orElseThrow();

        assertThat(loaded.getOrganizer().getId()).isEqualTo(organizerId);
        assertThat(loaded.getType().getId()).isEqualTo(type.getId());
        assertThat(loaded.getArtFields()).extracting(f -> f.getId()).containsExactly(artField.getId());
        assertThat(loaded.getTitle()).isEqualTo("Izložba");
        assertThat(loaded.getDescription()).isEqualTo("Opis izložbe");
        assertThat(loaded.getCriteria()).isEqualTo("Kriteriji");
        assertThat(loaded.getPrize()).isEqualTo("Nagrada");
        assertThat(loaded.getDeadline().toInstant()).isEqualTo(now.plusDays(10).toInstant());
        assertThat(loaded.getEventStart().toInstant()).isEqualTo(now.plusDays(20).toInstant());
        assertThat(loaded.getEventEnd().toInstant()).isEqualTo(now.plusDays(21).toInstant());
        assertThat(loaded.getCreatedAt().toInstant()).isEqualTo(now.toInstant());
        assertThat(loaded.getUpdatedAt().toInstant()).isEqualTo(now.toInstant());
        assertThat(loaded.getStatus()).isEqualTo(OpportunityStatus.PREPARATION);
        assertThat(loaded.isArchived()).isFalse();
        assertThat(loaded.isHiddenByAdmin()).isFalse();
        assertThat(loaded.getPublishedAt()).isNull();
        assertThat(loaded.getHiddenAt()).isNull();
        assertThat(loaded.getLocation().getType()).isEqualTo(locationType);
        if (locationType == LocationType.PHYSICAL) {
            assertThat(loaded.getLocation().getStreetAddress()).isEqualTo("Ilica 1");
            assertThat(loaded.getLocation().getCity()).isEqualTo("Zagreb");
            assertThat(loaded.getLocation().getPostalCode()).isEqualTo("10000");
            assertThat(loaded.getLocation().getLatitude()).isEqualByComparingTo("45.123457");
            assertThat(loaded.getLocation().getLongitude()).isEqualByComparingTo("16.123457");
            assertThat(loaded.getLocation().getOnlineUrl()).isNull();
        } else {
            assertThat(loaded.getLocation().getOnlineUrl()).isEqualTo("https://example.org/event");
            assertThat(loaded.getLocation().getStreetAddress()).isNull();
            assertThat(loaded.getLocation().getCity()).isNull();
            assertThat(loaded.getLocation().getPostalCode()).isNull();
            assertThat(loaded.getLocation().getLatitude()).isNull();
            assertThat(loaded.getLocation().getLongitude()).isNull();
        }
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM opportunity_art_fields WHERE opportunity_id = ? AND art_field_id = ?",
                Integer.class, id, artField.getId())).isEqualTo(1);

        var dto = mapper.toDto(loaded, profiles.findById(organizerId).orElseThrow());
        var json = objectMapper.readTree(objectMapper.writeValueAsString(dto));
        assertThat(json.get("id").asString()).isEqualTo(id.toString());
        assertThat(json.get("status").asString()).isEqualTo("PREPARATION");
        assertThat(json.get("organizer").get("displayName").asString()).isEqualTo("Test ORGANIZER");
        assertThat(json.get("organizer").get("organizationName").asString()).isEqualTo("Galerija");
        assertThat(json.get("type").get("code").asString()).isEqualTo(type.getCode());
        assertThat(json.get("artFields").size()).isEqualTo(1);
        assertThat(json.get("location").get("type").asString()).isEqualTo(locationType.name());
        assertThat(json.get("createdAt").isString()).isTrue();
        assertThat(json.get("hasResults").asBoolean()).isFalse();
        assertThat(json.get("myApplicationId").isNull()).isTrue();
        assertThat(json.get("publishedAt").isNull()).isTrue();
    }
}
