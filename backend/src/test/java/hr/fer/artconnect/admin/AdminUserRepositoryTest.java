package hr.fer.artconnect.admin;

import static org.assertj.core.api.Assertions.assertThat;

import hr.fer.artconnect.admin.domain.AdminUserRepository;
import hr.fer.artconnect.admin.service.AdminUserMapper;
import hr.fer.artconnect.auth.domain.OrganizerApproval;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest
@Testcontainers
@Transactional
class AdminUserRepositoryTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-10-01T10:00:00Z");
    private static final OffsetDateTime LAST_LOGIN_AT = CREATED_AT.plusHours(1);

    @Autowired
    AdminUserRepository users;

    @Autowired
    JdbcTemplate jdbc;

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
