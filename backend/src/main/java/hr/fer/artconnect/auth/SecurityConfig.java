package hr.fer.artconnect.auth;

import hr.fer.artconnect.common.*;
import org.springframework.context.*;
import org.springframework.context.annotation.*;
import org.springframework.core.env.*;
import org.springframework.http.*;
import org.springframework.security.config.annotation.method.configuration.*;
import org.springframework.security.config.annotation.web.builders.*;
import org.springframework.security.config.http.*;
import org.springframework.security.access.hierarchicalroles.*;
import org.springframework.security.access.expression.method.*;
import org.springframework.security.web.*;
import org.springframework.security.web.csrf.*;
import org.springframework.security.web.access.intercept.*;
import tools.jackson.databind.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, Environment environment,
            SecurityErrorHandler errors) throws Exception {
        // Samo navedene GET rute su javne; ostali zahtjevi traže prijavu.
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.GET,
                        "/api/v1/opportunities/**",
                        "/api/v1/artists/**",
                        "/api/v1/artworks/**",
                        "/api/v1/files/**",
                        "/api/v1/art-fields",
                        "/api/v1/opportunity-types",
                        "/api/v1/settings/public",
                        "/api/v1/auth/csrf",
                        "/actuator/health").permitAll()
                .anyRequest().authenticated());

        // Sesiju stvara Spring po potrebi, a Spring Session JDBC sprema je u bazu.
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
        // API ne treba spremati zahtjev radi preusmjeravanja nakon prijave.
        http.requestCache(cache -> cache.disable());

        // Frontend čita XSRF-TOKEN kolačić i šalje ga u zaglavlju X-XSRF-TOKEN.
        var tokens = CookieCsrfTokenRepository.withHttpOnlyFalse();
        tokens.setCookieCustomizer(cookie -> cookie.sameSite("Lax")
                .secure(environment.acceptsProfiles(Profiles.of("prod"))));
        http.csrf(csrf -> {
            csrf.csrfTokenRepository(tokens)
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler());
            // Razvojna prijava smije preskočiti CSRF samo u profilu dev.
            if (environment.acceptsProfiles(Profiles.of("dev & !prod"))) {
                csrf.ignoringRequestMatchers("/api/v1/dev/**");
            }
        });

        // Greške u filtrima nastaju prije kontrolera, pa im ovdje šaljemo JSON.
        http.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(errors)
                .accessDeniedHandler(errors));

        // Filter vidi prijavljenog korisnika prije odluke o pristupu ruti.
        // Ne označavamo ga kao @Component, kako se ne bi registrirao i izvan ovog lanca.
        http.addFilterBefore(new UserStatusFilter(), AuthorizationFilter.class);

        return http.build();
    }

    @Bean
    public SecurityErrorHandler securityErrorHandler(ObjectMapper mapper, MessageSource messages) {
        return new SecurityErrorHandler(mapper, messages);
    }

    @Bean
    public static RoleHierarchy roleHierarchy() {
        // Issue ne definira nasljeđivanje: ARTIST, ORGANIZER i ADMIN zasad imaju odvojena prava.
        return RoleHierarchyImpl.fromHierarchy("");
    }

    @Bean
    public static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy hierarchy) {
        // @PreAuthorize koristi istu konfiguraciju hijerarhije uloga.
        var handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(hierarchy);
        return handler;
    }
}
