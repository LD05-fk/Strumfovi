package hr.fer.artconnect.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import hr.fer.artconnect.auth.domain.OrganizerApproval;
import hr.fer.artconnect.auth.domain.OrganizerProfile;
import hr.fer.artconnect.auth.domain.OrganizerProfileRepository;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.auth.service.OrganizerProfileService;
import hr.fer.artconnect.auth.web.OrganizerProfileUpdateRequest;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrganizerProfileServiceTest {
    @Mock OrganizerProfileRepository organizerProfiles;
    @Mock CurrentUserProvider currentUserProvider;
    @Mock User user;
    @InjectMocks OrganizerProfileService service;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(user.getId()).thenReturn(userId);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(user.getDisplayName()).thenReturn("Test Organizer");
        when(user.getOrganizerApproval()).thenReturn(OrganizerApproval.PENDING);
    }

    @Test
    void obrezujeNazivPrijePohrane() {
        when(organizerProfiles.findById(userId)).thenReturn(Optional.of(profile("Galerija Kontrast")));

        var result = service.updateMyOrganizerProfile(
                new OrganizerProfileUpdateRequest("  Galerija Kontrast \t"));

        assertThat(result.organizationName()).isEqualTo("Galerija Kontrast");
        assertThat(result.organizerApproval()).isEqualTo(OrganizerApproval.PENDING);
        verify(organizerProfiles).upsert(userId, "Galerija Kontrast");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "\t "})
    void prazanNazivSeSpremaKaoNull(String organizationName) {
        when(organizerProfiles.findById(userId)).thenReturn(Optional.of(profile(null)));

        var result = service.updateMyOrganizerProfile(new OrganizerProfileUpdateRequest(organizationName));

        assertThat(result.organizationName()).isNull();
        verify(organizerProfiles).upsert(userId, null);
    }

    private OrganizerProfile profile(String organizationName) {
        OrganizerProfile profile = OrganizerProfile.createFor(user);
        profile.updateOrganizationName(organizationName);
        return profile;
    }
}
