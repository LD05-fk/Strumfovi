package hr.fer.artconnect.auth.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChooseRoleRequest(
        @NotBlank(message = "Uloga je obavezna.")
        @Pattern(regexp = "ARTIST|ORGANIZER", message = "Odaberite ulogu ARTIST ili ORGANIZER.")
        String role) {
}
