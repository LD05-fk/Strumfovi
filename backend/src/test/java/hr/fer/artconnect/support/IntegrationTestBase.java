package hr.fer.artconnect.support;

import hr.fer.artconnect.auth.domain.UserRepository;
import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.stream.Stream;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public abstract class IntegrationTestBase {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    static {
        POSTGRES.start();
    }

    private static final Set<String> KEEP_TABLES = Set.of(
            "databasechangelog",
            "databasechangeloglock",
            "system_settings",
            "art_fields",
            "opportunity_types");

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    // Tablice na koje upućuje neka zadržana tablica (system_settings.updated_by ->
    // users).
    // TRUNCATE ... CASCADE bi preko stranog ključa ispraznio i zadržanu tablicu,
    // pa se one prazne s DELETE.
    private static final Set<String> DELETE_TABLES = Set.of("users");

    @BeforeEach
    void cleanDatabase() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public'", String.class);
        String toTruncate = tables.stream()
                .filter(t -> !KEEP_TABLES.contains(t.toLowerCase()))
                .filter(t -> !DELETE_TABLES.contains(t.toLowerCase()))
                .map(t -> "\"" + t + "\"")
                .collect(Collectors.joining(", "));
        if (!toTruncate.isEmpty()) {
            jdbcTemplate.execute("TRUNCATE TABLE " + toTruncate + " RESTART IDENTITY CASCADE");
        }
        // Zadnje, kad su tablice koje upućuju na users već prazne.
        DELETE_TABLES.forEach(t -> jdbcTemplate.execute("DELETE FROM \"" + t + "\""));
    }

    @AfterEach
    void clearSecurity() {
        TestSecurityContextHolder.clearContext();
    }

    // CurrentUserProvider čita korisnika iz baze, pa prijavljeni korisnik mora imati svoj red u users.
    protected UUID loginAs(String role) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, display_name, role, status, organizer_approval) "
                        + "VALUES (?, ?, ?, ?, 'ACTIVE', ?)",
                id, id + "@artconnect.test", "Test " + role, role,
                "ORGANIZER".equals(role) ? "APPROVED" : null);
        var user = userRepository.findById(id).orElseThrow();
        TestSecurityContextHolder.setAuthentication(TestPrincipals.authentication(user));
        return id;
    }

    // Šalje token kao frontend: isti XSRF-TOKEN kolačić i X-XSRF-TOKEN zaglavlje.
    // Springov csrf() trajno zamijeni repozitorij tokena u CsrfFilteru keširanog
    // konteksta, pa bi poslije njega /auth/csrf vraćao krivo zaglavlje.
    protected RequestPostProcessor withCsrf() {
        return request -> {
            String token = UUID.randomUUID().toString();
            Cookie[] existing = request.getCookies() == null ? new Cookie[0] : request.getCookies();
            request.setCookies(Stream.concat(Stream.of(existing), Stream.of(new Cookie("XSRF-TOKEN", token)))
                    .toArray(Cookie[]::new));
            request.addHeader("X-XSRF-TOKEN", token);
            return request;
        };
    }
}