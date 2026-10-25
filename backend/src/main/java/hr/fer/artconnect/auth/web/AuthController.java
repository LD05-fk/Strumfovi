package hr.fer.artconnect.auth.web;

import hr.fer.artconnect.auth.CurrentUserProvider;
import hr.fer.artconnect.auth.SessionAuthenticationUpdater;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.service.RoleSelectionService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    private final CurrentUserProvider currentUserProvider;
    private final RoleSelectionService roleSelectionService;
    private final SessionAuthenticationUpdater sessionAuthenticationUpdater;

    public AuthController(CurrentUserProvider currentUserProvider, RoleSelectionService roleSelectionService,
            SessionAuthenticationUpdater sessionAuthenticationUpdater) {
        this.currentUserProvider = currentUserProvider;
        this.roleSelectionService = roleSelectionService;
        this.sessionAuthenticationUpdater = sessionAuthenticationUpdater;
    }

    @GetMapping("/api/v1/me")
    public CurrentUserDto me() {
        // Provider čita aktualne podatke iz baze, a ne kopiju spremljenu u sesiji.
        return CurrentUserDto.from(currentUserProvider.getCurrentUser());
    }

    @PostMapping("/api/v1/me/role")
    public CurrentUserDto chooseRole(@Valid @RequestBody ChooseRoleRequest body,
            HttpServletRequest request, HttpServletResponse response) {
        var user = roleSelectionService.chooseRole(UserRole.valueOf(body.role()));
        // Servis je već potvrdio transakciju; tek sada spremamo nova prava u sesiju.
        sessionAuthenticationUpdater.update(user.getId(), request, response);
        return CurrentUserDto.from(user);
    }
}
