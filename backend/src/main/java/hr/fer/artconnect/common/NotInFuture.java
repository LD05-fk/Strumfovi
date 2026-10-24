package hr.fer.artconnect.common;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Godina ne smije biti veća od tekuće godine u zoni {@link TimeConfiguration#APP_ZONE}.
 *
 * <p>Prazna vrijednost prolazi; obaveznost se izražava zasebnim {@code @NotNull}.
 */
@Documented
@Constraint(validatedBy = NotInFutureValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotInFuture {
    String message() default "{validation.year.notInFuture}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
