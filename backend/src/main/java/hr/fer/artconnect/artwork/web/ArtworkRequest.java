package hr.fer.artconnect.artwork.web;

import hr.fer.artconnect.artwork.domain.ArtworkVisibility;
import hr.fer.artconnect.common.NotInFuture;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Shema ArtworkInput. Vlasnik je uvijek prijavljeni umjetnik. */
public record ArtworkRequest(
        @NotBlank(message = "Naslov je obavezan.")
        @Size(max = 200, message = "Naslov smije imati najviše 200 znakova.")
        String title,
        @NotNull(message = "Godina je obavezna.")
        @Min(value = 1900, message = "Godina mora biti 1900. ili kasnija.")
        @NotInFuture
        Integer year,
        @NotBlank(message = "Tehnika je obavezna.")
        @Size(max = 200, message = "Tehnika smije imati najviše 200 znakova.")
        String technique,
        @Size(max = 100, message = "Dimenzije smiju imati najviše 100 znakova.")
        String dimensions,
        @Size(max = 5000, message = "Opis smije imati najviše 5000 znakova.")
        String description,
        @NotNull(message = "Vidljivost je obavezna.")
        ArtworkVisibility visibility
) {
    public ArtworkRequest {
        title = title == null ? null : title.strip();
        technique = technique == null ? null : technique.strip();
        dimensions = stripToNull(dimensions);
        description = stripToNull(description);
    }

    private static String stripToNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
