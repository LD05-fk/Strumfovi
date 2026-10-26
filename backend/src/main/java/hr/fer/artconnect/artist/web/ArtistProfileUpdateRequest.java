package hr.fer.artconnect.artist.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.hibernate.validator.constraints.UniqueElements;

/** Shema ArtistProfileUpdate. Sva polja tijela predstavljaju potpuno stanje profila. */
public record ArtistProfileUpdateRequest(
        @Size(max = 5000, message = "Biografija smije imati najviše 5000 znakova.")
        String biography,
        @Size(max = 5000, message = "Opis umjetničkog rada smije imati najviše 5000 znakova.")
        String artistStatement,
        @NotNull(message = "Područja umjetnosti su obavezna.")
        @Size(max = 10, message = "Možete odabrati najviše 10 područja umjetnosti.")
        @UniqueElements(message = "Područja umjetnosti ne smiju se ponavljati.")
        List<@NotNull(message = "Identifikator područja je obavezan.") UUID> artFieldIds,
        @NotNull(message = "Poveznice su obavezne.")
        @Size(max = 10, message = "Možete unijeti najviše 10 poveznica.")
        List<@NotNull @Valid ArtistLinkRequest> links) {

    public ArtistProfileUpdateRequest {
        biography = strip(biography);
        artistStatement = strip(artistStatement);
        artFieldIds = immutableCopy(artFieldIds);
        links = immutableCopy(links);
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }

    private static <T> List<T> immutableCopy(List<T> values) {
        return values == null ? null : Collections.unmodifiableList(new ArrayList<>(values));
    }

    public record ArtistLinkRequest(
            @Size(max = 60, message = "Oznaka poveznice smije imati najviše 60 znakova.")
            String label,
            @NotBlank(message = "URL poveznice je obavezan.")
            @Size(max = 1024, message = "URL poveznice smije imati najviše 1024 znakova.")
            String url) {
        public ArtistLinkRequest {
            label = strip(label);
            url = strip(url);
        }
    }
}
