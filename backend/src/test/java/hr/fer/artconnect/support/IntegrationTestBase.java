package hr.fer.artconnect.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import hr.fer.artconnect.auth.CurrentUser;
import java.util.List;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    protected void loginAs(String role) {
        CurrentUser principal = new CurrentUser(
                UUID.randomUUID(),
                CurrentUser.Role.valueOf(role),
                CurrentUser.Status.ACTIVE);
        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        TestSecurityContextHolder.setAuthentication(authentication);
    }

    protected RequestPostProcessor withCsrf() {
        return csrf();
    }
}