package hr.fer.artconnect.opportunity;

import static org.assertj.core.api.Assertions.assertThat;

import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.opportunity.domain.Location;
import hr.fer.artconnect.opportunity.domain.Opportunity;
import hr.fer.artconnect.opportunity.domain.OpportunityStatus;
import hr.fer.artconnect.opportunity.service.OpportunityVisibilityPolicy;
import hr.fer.artconnect.support.TestUsers;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class OpportunityVisibilityPolicyTest {
    private static final UUID ORGANIZER_ID = UUID.randomUUID();
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-10-26T10:00:00Z");
    private final OpportunityVisibilityPolicy policy = new OpportunityVisibilityPolicy();

    @Test
    void provjeravaSveKombinacijeStatusaZastavicaOrganizatoraIPozivatelja() {
        User anonymous = null;
        User otherOrganizer = user(UUID.randomUUID(), UserRole.ORGANIZER, UserStatus.ACTIVE);
        User owner = user(ORGANIZER_ID, UserRole.ORGANIZER, UserStatus.ACTIVE);
        User admin = user(UUID.randomUUID(), UserRole.ADMIN, UserStatus.ACTIVE);

        for (OpportunityStatus status : OpportunityStatus.values()) {
            for (boolean archived : new boolean[] {false, true}) {
                for (boolean hidden : new boolean[] {false, true}) {
                    for (UserStatus organizerStatus : new UserStatus[] {
                            UserStatus.ACTIVE, UserStatus.BLOCKED, UserStatus.DELETED}) {
                        User organizer = user(ORGANIZER_ID, UserRole.ORGANIZER, organizerStatus);
                        Opportunity opportunity = opportunity(organizer, status, archived, hidden);
                        boolean publiclyVisible = status != OpportunityStatus.PREPARATION
                                && !archived && !hidden && organizerStatus != UserStatus.BLOCKED;

                        assertThat(policy.canView(opportunity, anonymous))
                                .as("anonymous: %s, archived=%s, hidden=%s, organizer=%s",
                                        status, archived, hidden, organizerStatus)
                                .isEqualTo(publiclyVisible);
                        assertThat(policy.canView(opportunity, otherOrganizer))
                                .as("other organizer: %s, archived=%s, hidden=%s, organizer=%s",
                                        status, archived, hidden, organizerStatus)
                                .isEqualTo(publiclyVisible);
                        assertThat(policy.canView(opportunity, owner))
                                .as("owner: %s, archived=%s, hidden=%s, organizer=%s",
                                        status, archived, hidden, organizerStatus)
                                .isTrue();
                        assertThat(policy.canView(opportunity, admin))
                                .as("admin: %s, archived=%s, hidden=%s, organizer=%s",
                                        status, archived, hidden, organizerStatus)
                                .isTrue();
                    }
                }
            }
        }
    }

    private static Opportunity opportunity(User organizer, OpportunityStatus status,
            boolean archived, boolean hidden) {
        Opportunity opportunity = Opportunity.create(organizer, null, Set.of(),
                "Naslov", "Opis", NOW.plusDays(1), null, null,
                Location.online("https://example.org"), null, null, NOW);
        ReflectionTestUtils.setField(opportunity, "status", status);
        ReflectionTestUtils.setField(opportunity, "archived", archived);
        ReflectionTestUtils.setField(opportunity, "hiddenByAdmin", hidden);
        return opportunity;
    }

    private static User user(UUID id, UserRole role, UserStatus status) {
        return TestUsers.user(id, "sub-" + id, id + "@test", role, status);
    }
}
