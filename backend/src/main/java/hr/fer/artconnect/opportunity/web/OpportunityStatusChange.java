package hr.fer.artconnect.opportunity.web;

import jakarta.validation.constraints.NotNull;

public record OpportunityStatusChange(
        @NotNull(message = "Status prilike je obavezan.")
        Status status
) {
    // PREPARATION nastaje stvaranjem prilike, a FINISHED objavom rezultata.
    public enum Status {
        OPEN, CLOSED, SELECTION
    }
}
