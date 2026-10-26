package hr.fer.artconnect.opportunity.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Vlasnika, status i vrijeme stvaranja određuje backend, a ne klijent. */
public record OpportunityInput(
        @NotBlank(message = "Naslov je obavezan.")
        @Size(min = 3, max = 200, message = "Naslov mora imati od 3 do 200 znakova.")
        String title,
        @NotBlank(message = "Opis je obavezan.")
        @Size(min = 1, max = 10000, message = "Opis mora imati od 1 do 10000 znakova.")
        String description,
        @NotNull(message = "Vrsta prilike je obavezna.")
        UUID typeId,
        @NotNull(message = "Područja umjetnosti su obavezna.")
        @Size(min = 1, max = 10, message = "Odaberite od 1 do 10 područja umjetnosti.")
        List<@NotNull(message = "Područje umjetnosti ne smije biti prazno.") UUID> artFieldIds,
        @NotNull(message = "Rok prijave je obavezan.")
        OffsetDateTime deadline,
        @Size(max = 5000, message = "Kriteriji smiju imati najviše 5000 znakova.")
        String criteria,
        @Size(max = 2000, message = "Nagrada smije imati najviše 2000 znakova.")
        String prize,
        @NotNull(message = "Lokacija je obavezna.")
        @Valid
        LocationInput location,
        OffsetDateTime eventStart,
        OffsetDateTime eventEnd
) {
    public OpportunityInput {
        title = title == null ? null : title.strip();
        description = description == null ? null : description.strip();
        criteria = stripToNull(criteria);
        prize = stripToNull(prize);
    }

    private static String stripToNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
