package hr.fer.artconnect.auth;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.common.SecurityErrorHandler;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, Environment environment,
            SecurityErrorHandler errors, ArtConnectOidcUserService oidcUserService, SecurityContextRepository securityContextRepository) throws Exception {
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
        http.oauth2Login(oauth2 -> oauth2
            .userInfoEndpoint(userInfo -> userInfo.oidcUserService(oidcUserService))
            .successHandler((request, response, authentication) -> {
                Object principal = authentication.getPrincipal();

                if (principal instanceof ArtConnectPrincipal user
                        && user.getStatus() == UserStatus.ONBOARDING) {
                    response.sendRedirect("/onboarding");
                } else {
                    response.sendRedirect("/");
                }
            })
            .failureHandler((request, response, exception) ->
                    response.sendRedirect("/?login=error"))
        );
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
        http.securityContext(context -> context.securityContextRepository(securityContextRepository));
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
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
