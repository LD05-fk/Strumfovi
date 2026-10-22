package hr.fer.artconnect.catalog.web;

import jakarta.validation.constraints.*;

public record CatalogItemInput(
        @NotBlank(message = "Šifra je obavezna.")
        @Size(min = 2, max = 40, message = "Šifra mora imati od 2 do 40 znakova.")
        @Pattern(regexp = "^[a-z0-9-]+$", message = "Šifra smije sadržavati mala slova a–z, brojke i crticu.")
        String code,
        @NotBlank(message = "Naziv je obavezan.")
        @Size(min = 2, max = 80, message = "Naziv mora imati od 2 do 80 znakova.")
        String name,
        @NotNull(message = "Aktivnost je obavezna.")
        Boolean active,
        @NotNull(message = "Pozicija je obavezna.")
        @Min(value = 0, message = "Pozicija mora biti najmanje 0.")
        @Max(value = 999, message = "Pozicija smije biti najviše 999.")
        Integer position) {
}
