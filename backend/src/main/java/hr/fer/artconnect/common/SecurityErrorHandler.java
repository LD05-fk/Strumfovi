package hr.fer.artconnect.common;

import jakarta.servlet.http.*;
import java.io.*;
import java.util.*;
import org.springframework.context.*;
import org.springframework.http.*;
import org.springframework.security.access.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.*;
import org.springframework.security.web.*;
import org.springframework.security.web.access.*;
import org.springframework.security.web.csrf.*;
import tools.jackson.databind.*;

// I filtri i MVC koriste isti kod greške i istu JSON omotnicu.
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper mapper;
    private final MessageSource messages;

    public SecurityErrorHandler(ObjectMapper mapper, MessageSource messages) {
        this.mapper = mapper;
        this.messages = messages;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        write(response, ErrorCode.UNAUTHENTICATED);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException exception) throws IOException {
        write(response, accessDeniedCode(exception, SecurityContextHolder.getContext().getAuthentication()));
    }

    public static ErrorCode accessDeniedCode(AccessDeniedException exception, Authentication authentication) {
        // Neispravan ili izostavljen CSRF token uvijek znači 403, i bez prijave.
        if (exception instanceof CsrfException) {
            return ErrorCode.FORBIDDEN;
        }
        // Anonimni korisnik tek se treba prijaviti; prijavljeni nema potrebna prava.
        if (authentication == null || !authentication.isAuthenticated()
                || new AuthenticationTrustResolverImpl().isAnonymous(authentication)) {
            return ErrorCode.UNAUTHENTICATED;
        }
        return ErrorCode.FORBIDDEN;
    }

    private void write(HttpServletResponse response, ErrorCode code) throws IOException {
        String message = messages.getMessage("error." + code.name(), null, Locale.forLanguageTag("hr"));
        response.setStatus(code.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), new ErrorResponse(code, message, List.of()));
    }
}
