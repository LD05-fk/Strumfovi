package hr.fer.artconnect.opportunity;

import static org.assertj.core.api.Assertions.assertThat;

import hr.fer.artconnect.opportunity.domain.LocationType;
import hr.fer.artconnect.opportunity.web.LocationInput;
import hr.fer.artconnect.opportunity.web.OpportunityInput;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class OpportunityInputTest {
    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setup() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    @Test
    void obrezujeTekstoveIPripremaLokacijuPrijeValidacije() {
        var location = new LocationInput(LocationType.ONLINE, "x".repeat(201), "Grad", "10000",
                new BigDecimal("999"), new BigDecimal("999"), "  https://example.org  ");
        var input = input("  Naslov  ", "  Opis  ", List.of(UUID.randomUUID()), "  ", " Nagrada ", location);

        assertThat(validator.validate(input)).isEmpty();
        assertThat(input.title()).isEqualTo("Naslov");
        assertThat(input.description()).isEqualTo("Opis");
        assertThat(input.criteria()).isNull();
        assertThat(input.prize()).isEqualTo("Nagrada");
        assertThat(input.location().onlineUrl()).isEqualTo("https://example.org");
        assertThat(location.streetAddress()).isNull();
        assertThat(location.city()).isNull();
        assertThat(location.postalCode()).isNull();
        assertThat(location.latitude()).isNull();
        assertThat(location.longitude()).isNull();
    }

    @Test
    void provjeravaGraniceDuljinaIRasponKoordinataNaUgnijezdenimPoljima() {
        var location = new LocationInput(LocationType.PHYSICAL, "  Ilica 1  ", " Zagreb ", "10000",
                new BigDecimal("-90"), new BigDecimal("180"), "x".repeat(1025));
        var ids = new ArrayList<UUID>();
        for (int i = 0; i < 10; i++) {
            ids.add(UUID.randomUUID());
        }
        assertThat(validator.validate(input("a".repeat(200), "a".repeat(10000), ids,
                "a".repeat(5000), "a".repeat(2000), location))).isEmpty();
        assertThat(location.streetAddress()).isEqualTo("Ilica 1");
        assertThat(location.city()).isEqualTo("Zagreb");
        assertThat(location.onlineUrl()).isNull();

        ids.add(UUID.randomUUID());
        var invalidLocation = new LocationInput(LocationType.PHYSICAL, "a".repeat(201), "a".repeat(101),
                "a".repeat(11), new BigDecimal("90.0000001"), new BigDecimal("-180.0000001"), null);
        assertThat(validator.validate(input("a".repeat(201), "a".repeat(10001), ids,
                "a".repeat(5001), "a".repeat(2001), invalidLocation)))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("title", "description", "artFieldIds", "criteria", "prize",
                        "location.streetAddress", "location.city", "location.postalCode",
                        "location.latitude", "location.longitude");
    }

    @Test
    void provjeravaObaveznaPoljaIMinimalneDuljine() {
        var missing = new OpportunityInput(null, null, null, null, null, null, null, null, null, null);
        assertThat(validator.validate(missing)).extracting(v -> v.getPropertyPath().toString())
                .contains("title", "description", "typeId", "artFieldIds", "deadline", "location");
        assertThat(validator.validate(input(" ab ", " ", List.of(), null, null,
                new LocationInput(null, null, null, null, null, null, null))))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("title", "description", "artFieldIds", "location.type");
        assertThat(validator.validate(input("abc", "a", Arrays.asList((UUID) null), null, null,
                new LocationInput(LocationType.ONLINE, null, null, null, null, null, "a".repeat(1025)))))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("artFieldIds[0].<list element>", "location.onlineUrl");
    }

    private OpportunityInput input(String title, String description, List<UUID> ids,
            String criteria, String prize, LocationInput location) {
        return new OpportunityInput(title, description, UUID.randomUUID(), ids,
                OffsetDateTime.parse("2027-01-01T00:00:00Z"), criteria, prize, location, null, null);
    }
}
