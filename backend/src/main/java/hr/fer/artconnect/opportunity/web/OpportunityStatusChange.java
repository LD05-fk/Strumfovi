package hr.fer.artconnect.opportunity.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.NotNull;

public record OpportunityStatusChange(
        @NotNull(message = "Status prilike je obavezan.")
        Status status
) {
    // PREPARATION nastaje stvaranjem prilike, a FINISHED objavom rezultata.
    public enum Status {
        OPEN, CLOSED, SELECTION;

        // Bez ovoga Jackson prihvaća i broj kao redni broj enum vrijednosti.
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static Status fromValue(String value) {
            return Status.valueOf(value);
        }
    }
}
