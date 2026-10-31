package hr.fer.artconnect.opportunity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import hr.fer.artconnect.opportunity.domain.OpportunityStatus;
import hr.fer.artconnect.opportunity.domain.OpportunityStatusMachine;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class OpportunityStatusMachineTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-30T10:00:00Z");

    static Stream<Arguments> transitions() {
        // Svih 5 × 3 parova, s isteklim i budućim rokom.
        return Stream.of(OpportunityStatus.values()).flatMap(current ->
                Stream.of(OpportunityStatus.OPEN, OpportunityStatus.CLOSED, OpportunityStatus.SELECTION)
                        .flatMap(target -> Stream.of(-1, 1).map(seconds -> Arguments.of(current, target, seconds))));
    }

    @ParameterizedTest
    @MethodSource("transitions")
    void provjeravaSveParoveIRok(OpportunityStatus current, OpportunityStatus target, int seconds) {
        boolean allowed = (current == OpportunityStatus.PREPARATION && target == OpportunityStatus.OPEN)
                || (current == OpportunityStatus.OPEN && target == OpportunityStatus.CLOSED)
                || (current == OpportunityStatus.CLOSED
                    && (target == OpportunityStatus.OPEN || target == OpportunityStatus.SELECTION));
        ErrorCode expected = !allowed ? ErrorCode.INVALID_STATUS_TRANSITION
                : target == OpportunityStatus.OPEN && seconds < 0 ? ErrorCode.DEADLINE_PASSED : null;

        if (expected == null) {
            assertThatCode(() -> OpportunityStatusMachine.validate(current, target, false,
                    NOW.plusSeconds(seconds), NOW, true)).doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> OpportunityStatusMachine.validate(current, target, false,
                    NOW.plusSeconds(seconds), NOW, true)).isInstanceOfSatisfying(ApiException.class,
                            error -> assertThat(error.getCode()).isEqualTo(expected));
        }
    }

    @ParameterizedTest
    @MethodSource("transitions")
    void arhiviranaPrilikaUvijekOdbijaPromjenu(OpportunityStatus current, OpportunityStatus target, int seconds) {
        assertThatThrownBy(() -> OpportunityStatusMachine.validate(current, target, true,
                NOW.plusSeconds(seconds), NOW, true)).isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.getCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
    }

    @Test
    void rokJednakSadasnjemTrenutkuNeDopustaOtvaranjeNiUzDrugiPomak() {
        for (var current : new OpportunityStatus[] {OpportunityStatus.PREPARATION, OpportunityStatus.CLOSED}) {
            assertThatThrownBy(() -> OpportunityStatusMachine.validate(current, OpportunityStatus.OPEN, false,
                    NOW.withOffsetSameInstant(ZoneOffset.ofHours(2)), NOW, true))
                    .isInstanceOfSatisfying(ApiException.class,
                            error -> assertThat(error.getCode()).isEqualTo(ErrorCode.DEADLINE_PASSED));
        }
    }

    @Test
    void pripremljenaProvjeraOdobrenjaVrijediSamoPriPrvojObjavi() {
        assertThatThrownBy(() -> OpportunityStatusMachine.validate(OpportunityStatus.PREPARATION,
                OpportunityStatus.OPEN, false, NOW.plusDays(1), NOW, false))
                .isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.getCode()).isEqualTo(ErrorCode.ORGANIZER_NOT_APPROVED));
        assertThatCode(() -> OpportunityStatusMachine.validate(OpportunityStatus.CLOSED,
                OpportunityStatus.OPEN, false, NOW.plusDays(1), NOW, false)).doesNotThrowAnyException();
    }

    @Test
    void pripremaIZavrsetakNisuDopusteniCiljeviNiIzravnimPozivom() {
        for (var target : new OpportunityStatus[] {OpportunityStatus.PREPARATION, OpportunityStatus.FINISHED}) {
            assertThatThrownBy(() -> OpportunityStatusMachine.validate(OpportunityStatus.SELECTION,
                    target, false, NOW.plusDays(1), NOW, true)).isInstanceOfSatisfying(ApiException.class,
                            error -> assertThat(error.getCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION));
        }
    }
}
