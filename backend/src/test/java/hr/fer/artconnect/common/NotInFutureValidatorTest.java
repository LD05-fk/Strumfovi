package hr.fer.artconnect.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NotInFutureValidatorTest {
    // 31. 12. 2026. u 23:59 po zagrebačkom vremenu.
    private static final Instant STARA_GODINA = Instant.parse("2026-12-31T22:59:00Z");
    // Pola sata kasnije: u Zagrebu je već 1. 1. 2027., a po UTC-u još 31. 12. 2026.
    private static final Instant NOVA_GODINA = Instant.parse("2026-12-31T23:30:00Z");

    @ParameterizedTest
    @CsvSource({
            "1900, true",
            "2026, true",
            "2027, false",
            "9999, false"
    })
    void naStaruGodinuPrihvacaTekucuAOdbijaSljedecuGodinu(int year, boolean expected) {
        assertThat(validator(STARA_GODINA).isValid(year, null)).isEqualTo(expected);
    }

    @Test
    void poslijePonociUZagrebuPrihvacaNovuGodinuIakoJeUtcJosUStaroj() {
        // Ovo pada ako se tekuća godina čita u UTC-u umjesto u zoni Europe/Zagreb.
        assertThat(validator(NOVA_GODINA).isValid(2027, null)).isTrue();
    }

    @Test
    void poUtcuBiNovaGodinaBilaUBuducnosti() {
        // Isti trenutak, ali sat u UTC-u: potvrđuje da je zona sata ono što odlučuje.
        var utcValidator = new NotInFutureValidator(Clock.fixed(NOVA_GODINA, ZoneOffset.UTC));
        assertThat(utcValidator.isValid(2027, null)).isFalse();
    }

    @Test
    void praznuVrijednostPrepustaNotNullu() {
        assertThat(validator(STARA_GODINA).isValid(null, null)).isTrue();
    }

    private static NotInFutureValidator validator(Instant now) {
        return new NotInFutureValidator(Clock.fixed(now, TimeConfiguration.APP_ZONE));
    }
}
