package hr.fer.artconnect.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import org.springframework.web.filter.*;

public class UserStatusFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        // EPIC-01 ovdje dodaje ONBOARDING_REQUIRED, a EPIC-08 USER_BLOCKED.
        // Zasad samo propuštamo zahtjev; stvarna pravila pripadaju tim zadacima.
        chain.doFilter(request, response);
    }
}
