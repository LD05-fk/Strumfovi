package hr.fer.artconnect.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import hr.fer.artconnect.admin.domain.AdminUserRepository;
import hr.fer.artconnect.admin.service.AdminUserMapper;
import hr.fer.artconnect.auth.domain.OrganizerApproval;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@WithMockUser(roles = "ADMIN")
class AdminUserIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-10-01T10:00:00Z");
    private static final OffsetDateTime LAST_LOGIN_AT = CREATED_AT.plusHours(1);

    @Autowired
    AdminUserRepository users;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MockMvc mvc;

    UUID pendingOrganizer;
    UUID deletedUser;
    UUID onboardingUser;

    @BeforeEach
    void pripremiKorisnike() {
        jdbc.update("DELETE FROM organizer_profiles");
        jdbc.update("DELETE FROM users");

        insertUser("Ana Slikarica", "ana@example.test", "ARTIST", "ACTIVE", null, 0);
        pendingOrganizer = insertUser("Marko Horvat", "galerija@example.test",
                "ORGANIZER", "ACTIVE", "PENDING", 1);
        jdbc.update("INSERT INTO organizer_profiles (user_id, organization_name) VALUES (?, ?)",
                pendingOrganizer, "Galerija Marko");
        insertUser("Marko Odobren", "odobren@example.test", "ORGANIZER", "ACTIVE", "APPROVED", 2);
        insertUser("Marko Blokiran", "blokiran@example.test", "ORGANIZER", "BLOCKED", "PENDING", 3);
        deletedUser = insertUser("Obrisani korisnik", null, "ARTIST", "DELETED", null, 4);
        onboardingUser = insertUser("Novi 100%_korisnik", "novi@example.test", null, "ONBOARDING", null, 5);
    }

    @Test
    void kombiniraSveFiltreIPretrazujeImeIEmailBezObziraNaVelicinuSlova() {
        for (String q : new String[]{"hORv", "GALERIJA@"}) {
            var page = users.findUsers(q, UserRole.ORGANIZER, UserStatus.ACTIVE,
                    OrganizerApproval.PENDING, PageRequest.of(0, 1));

            assertThat(page.getTotalElements()).isEqualTo(1);
            var row = page.getContent().getFirst();
            var user = AdminUserMapper.toAdminUser(row.getUser(), row.getOrganizationName());
            assertThat(user.id()).isEqualTo(pendingOrganizer);
            assertThat(user.displayName()).isEqualTo("Marko Horvat");
            assertThat(user.email()).isEqualTo("galerija@example.test");
            assertThat(user.role()).isEqualTo(UserRole.ORGANIZER);
            assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
            assertThat(user.organizerApproval()).isEqualTo(OrganizerApproval.PENDING);
            assertThat(user.organizationName()).isEqualTo("Galerija Marko");
            assertThat(user.createdAt()).isEqualTo(CREATED_AT.plusDays(1));
            assertThat(user.lastLoginAt()).isEqualTo(LAST_LOGIN_AT);
        }

        var mismatch = users.findUsers("Ana", UserRole.ORGANIZER, null,
                null, PageRequest.of(0, 20));
        assertThat(mismatch).isEmpty();
        assertThat(mismatch.getTotalElements()).isZero();
    }

    @Test
    void straniciOdNajnovijihIUkljucujeKorisnikeBezProfilaIObrisane() {
        var first = users.findUsers(null, null, null, null, PageRequest.of(0, 2));
        assertThat(first.getTotalElements()).isEqualTo(6);
        assertThat(first.getContent()).extracting(row -> row.getUser().getId())
                .containsExactly(onboardingUser, deletedUser);

        var row = first.getContent().getFirst();
        var onboarding = AdminUserMapper.toAdminUser(row.getUser(), row.getOrganizationName());
        assertThat(onboarding.role()).isNull();
        assertThat(onboarding.organizerApproval()).isNull();
        assertThat(onboarding.organizationName()).isNull();
        assertThat(onboarding.lastLoginAt()).isNull();

        var deletedRow = first.getContent().get(1);
        var deleted = AdminUserMapper.toAdminUser(deletedRow.getUser(), deletedRow.getOrganizationName());
        assertThat(deleted.email()).isNull();
        assertThat(deleted.status()).isEqualTo(UserStatus.DELETED);

        var second = users.findUsers(null, null, null, null, PageRequest.of(1, 2));
        assertThat(second.getTotalElements()).isEqualTo(6);
        assertThat(second.getContent()).extracting(result -> result.getUser().getDisplayName())
                .containsExactly("Marko Blokiran", "Marko Odobren");

        var beyondEnd = users.findUsers(null, null, null, null, PageRequest.of(3, 2));
        assertThat(beyondEnd).isEmpty();
        assertThat(beyondEnd.getTotalElements()).isEqualTo(6);
    }

    @Test
    void pojedinacniFiltriIPretragaPosebnihZnakovaRadeNeovisno() {
        assertThat(users.findUsers(null, UserRole.ORGANIZER, null, null, PageRequest.of(0, 20)))
                .hasSize(3);
        assertThat(users.findUsers(null, null, UserStatus.ACTIVE, null, PageRequest.of(0, 20)))
                .hasSize(3);
        assertThat(users.findUsers(null, null, null, OrganizerApproval.PENDING, PageRequest.of(0, 20)))
                .hasSize(2);
        assertThat(users.findUsers(null, null, UserStatus.DELETED, null, PageRequest.of(0, 20)))
                .hasSize(1);
        assertThat(users.findUsers("%_", null, null, null, PageRequest.of(0, 20)).getContent())
                .extracting(row -> row.getUser().getId()).containsExactly(onboardingUser);
        assertThat(users.findUsers("", null, null, null, PageRequest.of(0, 20)))
                .hasSize(6);
        assertThat(users.findUsers("OBRISANI", null, null, null, PageRequest.of(0, 20)).getContent())
                .extracting(row -> row.getUser().getId()).containsExactly(deletedUser);
    }

    @Test
    void ukupanBrojIStranicePostujuFiltreIPretragu() {
        for (int pageNumber = 0; pageNumber < 2; pageNumber++) {
            var page = users.findUsers("mARKo", UserRole.ORGANIZER, null,
                    OrganizerApproval.PENDING, PageRequest.of(pageNumber, 1));

            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getTotalPages()).isEqualTo(2);
            assertThat(page.getContent()).extracting(row -> row.getUser().getDisplayName())
                    .containsExactly(pageNumber == 0 ? "Marko Blokiran" : "Marko Horvat");
        }
    }

    @Test
    void jednakiDatumiImajuStabilanRedoslijedPoId() {
        UUID lowerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID higherId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        for (UUID id : new UUID[]{lowerId, higherId}) {
            jdbc.update("INSERT INTO users (id, display_name, created_at) VALUES (?, ?, ?)",
                    id, "Jednako vrijeme", CREATED_AT);
        }

        var first = users.findUsers("Jednako", null, null, null, PageRequest.of(0, 1));
        var second = users.findUsers("Jednako", null, null, null, PageRequest.of(1, 1));

        assertThat(first.getContent()).extracting(row -> row.getUser().getId()).containsExactly(higherId);
        assertThat(second.getContent()).extracting(row -> row.getUser().getId()).containsExactly(lowerId);
    }

    @Test
    void endpointVracaFiltriraneKorisnikeIUgovoreniOblikOdgovora() throws Exception {
        mvc.perform(get("/api/v1/admin/users")
                        .param("q", "GALERIJA@")
                        .param("role", "ORGANIZER")
                        .param("status", "ACTIVE")
                        .param("organizerApproval", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].*", hasSize(9)))
                .andExpect(jsonPath("$.data[0].id").value(pendingOrganizer.toString()))
                .andExpect(jsonPath("$.data[0].displayName").value("Marko Horvat"))
                .andExpect(jsonPath("$.data[0].email").value("galerija@example.test"))
                .andExpect(jsonPath("$.data[0].role").value("ORGANIZER"))
                .andExpect(jsonPath("$.data[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data[0].organizerApproval").value("PENDING"))
                .andExpect(jsonPath("$.data[0].organizationName").value("Galerija Marko"))
                .andExpect(jsonPath("$.data[0].createdAt")
                        .value(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(CREATED_AT.plusDays(1))))
                .andExpect(jsonPath("$.data[0].lastLoginAt")
                        .value(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(LAST_LOGIN_AT)))
                .andExpect(jsonPath("$.meta.page").value(1))
                .andExpect(jsonPath("$.meta.pageSize").value(20))
                .andExpect(jsonPath("$.meta.total").value(1));
    }

    @Test
    void endpointVracaSveStatuseIStraniciOdJedinice() throws Exception {
        mvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(6)))
                .andExpect(jsonPath("$.data[0].id").value(onboardingUser.toString()))
                .andExpect(jsonPath("$.data[0].role").value(nullValue()))
                .andExpect(jsonPath("$.data[0].organizationName").value(nullValue()))
                .andExpect(jsonPath("$.data[1].id").value(deletedUser.toString()))
                .andExpect(jsonPath("$.data[1].email").value(nullValue()))
                .andExpect(jsonPath("$.meta.total").value(6));

        mvc.perform(get("/api/v1/admin/users").param("q", "marko")
                        .param("organizerApproval", "PENDING").param("page", "2").param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(pendingOrganizer.toString()))
                .andExpect(jsonPath("$.meta.page").value(2))
                .andExpect(jsonPath("$.meta.pageSize").value(1))
                .andExpect(jsonPath("$.meta.total").value(2));

        mvc.perform(get("/api/v1/admin/users").param("page", "99").param("pageSize", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.meta.total").value(6));
    }

    @ParameterizedTest
    @CsvSource({"role,NEPOZNATO", "status,NEPOZNATO", "organizerApproval,NEPOZNATO",
            "page,0", "page,-1", "page,abc", "pageSize,0", "pageSize,101", "pageSize,abc"})
    void endpointOdbijaNeispravneParametreSDetaljemPoPolju(String field, String value) throws Exception {
        mvc.perform(get("/api/v1/admin/users").param(field, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value(field))
                .andExpect(jsonPath("$.error.details[0].issue").isNotEmpty());
    }

    @Test
    void endpointProvjeravaGranicuDuljinePretrage() throws Exception {
        mvc.perform(get("/api/v1/admin/users").param("q", "a".repeat(100)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.meta.total").value(0));

        mvc.perform(get("/api/v1/admin/users").param("q", "a".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[0].field").value("q"))
                .andExpect(jsonPath("$.error.details[0].issue")
                        .value("Pretraga smije sadržavati najviše 100 znakova."));
    }

    @Test
    @WithAnonymousUser
    void endpointTraziPrijavu() throws Exception {
        mvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ARTIST", "ORGANIZER"})
    void endpointOdbijaKorisnikeBezAdministratorskeUloge(String role) throws Exception {
        mvc.perform(get("/api/v1/admin/users")
                        .with(user("korisnik").roles(role)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    private UUID insertUser(String name, String email, String role, String status, String approval, int day) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, display_name, email, role, status, organizer_approval, created_at, last_login_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, id, name, email, role, status, approval, CREATED_AT.plusDays(day),
                "ACTIVE".equals(status) ? LAST_LOGIN_AT : null);
        return id;
    }
}
