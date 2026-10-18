package hr.fer.artconnect.auth.web;

import org.springframework.security.web.csrf.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class CsrfController {
    @GetMapping("/api/v1/auth/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        // Čitanje tokena pokreće njegovo stvaranje i postavlja XSRF-TOKEN kolačić.
        return new CsrfResponse(token.getHeaderName(), token.getToken());
    }

    public record CsrfResponse(String headerName, String token) {
    }
}
