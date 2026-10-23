package hr.fer.artconnect.auth;

import static org.assertj.core.api.Assertions.assertThat;

import hr.fer.artconnect.auth.domain.OrganizerApproval;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.auth.web.CurrentUserDto;
import hr.fer.artconnect.support.TestUsers;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CurrentUserDtoTest {
    @Test
    void mapiraPodatkeOrganizatoraIOnboardinga() {
        var id = UUID.randomUUID();
        var user = TestUsers.user(id, "google-test", "test@example.com", UserRole.ORGANIZER, UserStatus.ACTIVE);
        user.updateGoogleProfile("test@example.com", "Novo ime", "https://example.com/avatar.png");
        ReflectionTestUtils.setField(user, "organizerApproval", OrganizerApproval.PENDING);

        assertThat(CurrentUserDto.from(user)).isEqualTo(new CurrentUserDto(id, "Novo ime", "test@example.com",
                "https://example.com/avatar.png", UserRole.ORGANIZER, UserStatus.ACTIVE, OrganizerApproval.PENDING));

        var onboarding = TestUsers.user(id, "google-test", "test@example.com", null, UserStatus.ONBOARDING);
        assertThat(CurrentUserDto.from(onboarding)).isEqualTo(new CurrentUserDto(id, "Test korisnik",
                "test@example.com", null, null, UserStatus.ONBOARDING, null));
    }
}
