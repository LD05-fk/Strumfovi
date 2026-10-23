package hr.fer.artconnect.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import hr.fer.artconnect.auth.domain.*;
import hr.fer.artconnect.auth.service.RoleSelectionService;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import hr.fer.artconnect.support.TestUsers;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleSelectionServiceTest {
    @Mock CurrentUserProvider currentUserProvider;
    @Mock UserRepository users;
    @Mock OrganizerProfileRepository organizerProfiles;
    @InjectMocks RoleSelectionService service;

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"ARTIST", "ORGANIZER"})
    void odabireUloguIStvaraSamoOrganizatorskiProfil(UserRole role) {
        UUID id = UUID.randomUUID();
        var before = TestUsers.user(id, "google", "test@example.com", null, UserStatus.ONBOARDING);
        var after = TestUsers.user(id, "google", "test@example.com", role, UserStatus.ACTIVE);
        var approval = role == UserRole.ORGANIZER ? OrganizerApproval.APPROVED : null;
        when(currentUserProvider.getCurrentUser()).thenReturn(before);
        when(users.chooseRole(eq(id), eq(role), eq(approval), any(OffsetDateTime.class))).thenReturn(1);
        when(users.findById(id)).thenReturn(Optional.of(after));

        assertThat(service.chooseRole(role)).isSameAs(after);

        if (role == UserRole.ORGANIZER) {
            var profile = ArgumentCaptor.forClass(OrganizerProfile.class);
            verify(organizerProfiles).save(profile.capture());
            assertThat(profile.getValue().getOrganizationName()).isNull();
        } else {
            verifyNoInteractions(organizerProfiles);
        }
    }

    @Test
    void bezAzuriranogRetkaVraca409INeStvaraProfil() {
        UUID id = UUID.randomUUID();
        when(currentUserProvider.getCurrentUser()).thenReturn(
                TestUsers.user(id, "google", "test@example.com", null, UserStatus.ONBOARDING));
        when(users.chooseRole(eq(id), eq(UserRole.ORGANIZER), eq(OrganizerApproval.APPROVED),
                any(OffsetDateTime.class))).thenReturn(0);

        assertThatThrownBy(() -> service.chooseRole(UserRole.ORGANIZER))
                .isInstanceOfSatisfying(ApiException.class,
                        error -> assertThat(error.getCode()).isEqualTo(ErrorCode.ROLE_ALREADY_CHOSEN));
        verify(users, never()).findById(any());
        verifyNoInteractions(organizerProfiles);
    }

    @Test
    void odbijaAdminINullPrijePromjeneBaze() {
        for (UserRole role : new UserRole[] {UserRole.ADMIN, null}) {
            assertThatThrownBy(() -> service.chooseRole(role)).isInstanceOfSatisfying(ApiException.class,
                    error -> assertThat(error.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR));
        }
        verifyNoInteractions(currentUserProvider, users, organizerProfiles);
    }
}
