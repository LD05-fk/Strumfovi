package hr.fer.artconnect.common;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Clock;
import java.time.Year;
import org.springframework.beans.factory.annotation.Qualifier;

/** Tekuća godina dolazi iz sata, kako bi se granica 31. 12. / 1. 1. mogla ispitati. */
public class NotInFutureValidator implements ConstraintValidator<NotInFuture, Integer> {
    private final Clock clock;

    public NotInFutureValidator(@Qualifier("zagrebClock") Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean isValid(Integer year, ConstraintValidatorContext context) {
        // Nedostajuću vrijednost prijavljuje @NotNull, inače bi korisnik dobio dvije greške za isto polje.
        return year == null || year <= Year.now(clock).getValue();
    }
}
