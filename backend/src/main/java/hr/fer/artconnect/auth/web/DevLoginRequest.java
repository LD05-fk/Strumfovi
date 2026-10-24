package hr.fer.artconnect.auth.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DevLoginRequest(
        @NotBlank
        @Email
        @Size(max = 254)
        String email) {
}
