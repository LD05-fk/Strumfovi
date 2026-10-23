package hr.fer.artconnect.auth.web;

import hr.fer.artconnect.auth.CurrentUserProvider;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.service.RoleSelectionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {
    private final CurrentUserProvider currentUserProvider;
    private final RoleSelectionService roleSelectionService;

    public AuthController(CurrentUserProvider currentUserProvider, RoleSelectionService roleSelectionService) {
        this.currentUserProvider = currentUserProvider;
        this.roleSelectionService = roleSelectionService;
    }

    @GetMapping("/api/v1/me")
    public CurrentUserDto me() {
        // Provider čita aktualne podatke iz baze, a ne kopiju spremljenu u sesiji.
        return CurrentUserDto.from(currentUserProvider.getCurrentUser());
    }

    @PostMapping("/api/v1/me/role")
    public CurrentUserDto chooseRole(@Valid @RequestBody ChooseRoleRequest request) {
        return CurrentUserDto.from(roleSelectionService.chooseRole(UserRole.valueOf(request.role())));
    }
}
