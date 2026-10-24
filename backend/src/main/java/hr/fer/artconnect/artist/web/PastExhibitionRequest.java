package hr.fer.artconnect.artist.web;

import hr.fer.artconnect.common.NotInFuture;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Shema PastExhibitionInput. Vlasnik je uvijek prijavljeni umjetnik. */
public record PastExhibitionRequest(
        @NotBlank(message = "Naslov je obavezan.")
        @Size(max = 200, message = "Naslov smije imati najviše 200 znakova.")
        String title,
        @NotNull(message = "Godina je obavezna.")
        @Min(value = 1900, message = "Godina mora biti 1900. ili kasnija.")
        @NotInFuture
        Integer year,
        @NotBlank(message = "Mjesto je obavezno.")
        @Size(max = 200, message = "Mjesto smije imati najviše 200 znakova.")
        String place
) {
    public PastExhibitionRequest {
        title = title == null ? null : title.strip();
        place = place == null ? null : place.strip();
    }
}
