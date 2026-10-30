package hr.fer.artconnect.auth.web;

import jakarta.validation.constraints.Size;

public record OrganizerProfileUpdateRequest(
        @Size(max = 200, message = "Naziv organizacije smije imati najviše 200 znakova.")
        String organizationName) {
}
