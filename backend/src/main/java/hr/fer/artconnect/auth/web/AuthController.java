package hr.fer.artconnect.auth.web;

import hr.fer.artconnect.auth.CurrentUserProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    private final CurrentUserProvider currentUserProvider;

    public AuthController(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/api/v1/me")
    public CurrentUserDto me() {
        // Provider čita aktualne podatke iz baze, a ne kopiju spremljenu u sesiji.
        return CurrentUserDto.from(currentUserProvider.getCurrentUser());
    }
}
