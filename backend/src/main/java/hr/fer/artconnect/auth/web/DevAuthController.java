package hr.fer.artconnect.auth.web;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hr.fer.artconnect.auth.SessionAuthenticationUpdater;
import hr.fer.artconnect.auth.service.DevLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/dev")
@Profile("dev & !prod")
public class DevAuthController {
    private final DevLoginService devLoginService;
    private final SessionAuthenticationUpdater sessionAuthenticationUpdater;

    public DevAuthController(
            DevLoginService devLoginService,
            SessionAuthenticationUpdater sessionAuthenticationUpdater) {
        this.devLoginService = devLoginService;
        this.sessionAuthenticationUpdater = sessionAuthenticationUpdater;
    }

    @PostMapping("/login-as")
    public CurrentUserDto loginAs(
            @Valid @RequestBody DevLoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        var user = devLoginService.loginAs(loginRequest.email());

        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        sessionAuthenticationUpdater.update(user.getId(), request, response);

        return CurrentUserDto.from(user);
    }
}
