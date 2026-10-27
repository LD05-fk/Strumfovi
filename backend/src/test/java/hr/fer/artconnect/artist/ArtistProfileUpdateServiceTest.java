package hr.fer.artconnect.artist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hr.fer.artconnect.artist.domain.ArtistLink;
import hr.fer.artconnect.artist.domain.ArtistLinkRepository;
import hr.fer.artconnect.artist.domain.ArtistProfile;
import hr.fer.artconnect.artist.domain.ArtistProfileRepository;
import hr.fer.artconnect.artist.service.ArtistProfileService;
import hr.fer.artconnect.artist.web.ArtistProfileDto;
import hr.fer.artconnect.artist.web.ArtistProfileUpdateRequest;
import hr.fer.artconnect.auth.CurrentUserProvider;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.catalog.domain.ArtField;
import hr.fer.artconnect.catalog.domain.ArtFieldRepository;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import hr.fer.artconnect.support.TestUsers;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ArtistProfileUpdateServiceTest {
    private static final UUID ARTIST_ID = UUID.randomUUID();

    @Mock
    ArtistProfileRepository artistProfiles;

    @Mock
    ArtistLinkRepository artistLinks;

    @Mock
    ArtFieldRepository artFields;

    @Mock
    CurrentUserProvider currentUserProvider;

    @Captor
    ArgumentCaptor<List<ArtistLink>> savedLinks;

    @Captor
    ArgumentCaptor<ArtistProfile> savedProfile;

    @Test
    void stvaraProfilAkoJosNePostoji() {
        when(currentUserProvider.getCurrentUser()).thenReturn(artist());
        when(artistProfiles.findById(ARTIST_ID)).thenReturn(Optional.empty());
        when(artistProfiles.save(any())).thenAnswer(invocation -> {
            ArtistProfile profile = invocation.getArgument(0);
            ReflectionTestUtils.setField(profile, "userId", ARTIST_ID);
            return profile;
        });
        when(artFields.findAllById(List.of())).thenReturn(List.of());
        when(artistLinks.findAllForArtistProfile(ARTIST_ID)).thenReturn(List.of());
        when(artistLinks.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service().updateMyArtistProfile(new ArtistProfileUpdateRequest(
                "Biografija", null, List.of(), List.of()));

        verify(artistProfiles).save(savedProfile.capture());
        assertThat(savedProfile.getValue().getUserId()).isEqualTo(ARTIST_ID);
        assertThat(savedProfile.getValue().getBiography()).isEqualTo("Biografija");
        assertThat(savedProfile.getValue().getArtistStatement()).isNull();
        assertThat(savedProfile.getValue().getArtFields()).isEmpty();
        assertThat(response.biography()).isEqualTo("Biografija");
        assertThat(response.artFields()).isEmpty();
        assertThat(response.links()).isEmpty();
    }

    @Test
    void zamjenjujePodrucjaIPoveznicePoredanimPoIndeksuIcuvaVecVezanoNeaktivnoPodrucje() {
        User user = artist();
        ArtistProfile profile = mock(ArtistProfile.class);
        ArtField inactiveField = artField("00000000-0000-0000-0000-000000000001", false);
        ArtField activeField = artField("00000000-0000-0000-0000-000000000002", true);
        when(profile.getArtFields()).thenReturn(new LinkedHashSet<>(List.of(inactiveField)));
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(artistProfiles.findById(ARTIST_ID)).thenReturn(Optional.of(profile));
        when(artFields.findAllById(List.of(inactiveField.getId(), activeField.getId())))
                .thenReturn(List.of(inactiveField, activeField));
        when(artistLinks.findAllForArtistProfile(ARTIST_ID)).thenReturn(List.of());
        when(artistLinks.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var request = new ArtistProfileUpdateRequest(
                "  Biografija  ",
                "  Opis rada  ",
                List.of(inactiveField.getId(), activeField.getId()),
                List.of(
                        new ArtistProfileUpdateRequest.ArtistLinkRequest("   ", " https://example.test/first "),
                        new ArtistProfileUpdateRequest.ArtistLinkRequest(" Instagram ", "http://example.test/second")));

        var response = service().updateMyArtistProfile(request);

        verify(profile).replace("Biografija", "Opis rada", List.of(inactiveField, activeField));
        verify(artistLinks).saveAll(savedLinks.capture());
        List<ArtistLink> links = savedLinks.getValue();
        assertThat(links).hasSize(2);
        assertThat(links).extracting(ArtistLink::getPosition).containsExactly((short) 0, (short) 1);
        assertThat(links).extracting(ArtistLink::getLabel).containsExactly(null, "Instagram");
        assertThat(links).extracting(ArtistLink::getUrl)
                .containsExactly("https://example.test/first", "http://example.test/second");
        assertThat(response.links()).extracting(ArtistProfileDto.ArtistLink::label)
                .containsExactly(null, "Instagram");
    }

    @Test
    void odbijaNovoNeaktivnoPodrucjeINeizmjenjujeProfil() {
        User user = artist();
        ArtistProfile profile = mock(ArtistProfile.class);
        ArtField inactiveField = artField("00000000-0000-0000-0000-000000000003", false);
        when(profile.getArtFields()).thenReturn(new LinkedHashSet<>());
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(artistProfiles.findById(ARTIST_ID)).thenReturn(Optional.of(profile));
        when(artFields.findAllById(List.of(inactiveField.getId()))).thenReturn(List.of(inactiveField));

        assertThatThrownBy(() -> service().updateMyArtistProfile(new ArtistProfileUpdateRequest(
                null, null, List.of(inactiveField.getId()), List.of())))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    assertThat(exception.getDetails()).extracting("field").containsExactly("artFieldIds");
                });

        verify(profile, never()).replace(any(), any(), any());
        verify(artistLinks, never()).saveAll(any());
    }

    @Test
    void odbijaUrlBezHttpShemeIliHostaUzPutanjuPoveznice() {
        User user = artist();
        ArtistProfile profile = mock(ArtistProfile.class);
        when(profile.getArtFields()).thenReturn(new LinkedHashSet<>());
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(artistProfiles.findById(ARTIST_ID)).thenReturn(Optional.of(profile));
        when(artFields.findAllById(List.of())).thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> service().updateMyArtistProfile(new ArtistProfileUpdateRequest(
                null,
                null,
                List.of(),
                List.of(new ArtistProfileUpdateRequest.ArtistLinkRequest("Web", "https://")))))
                .isInstanceOfSatisfying(ApiException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
                    assertThat(exception.getDetails()).extracting("field").containsExactly("links[0].url");
                });

        verify(profile, never()).replace(any(), any(), any());
    }

    private ArtistProfileService service() {
        return new ArtistProfileService(artistProfiles, artistLinks, artFields, currentUserProvider);
    }

    private static User artist() {
        return TestUsers.user(ARTIST_ID, "google-umjetnik", "umjetnik@artconnect.test",
                UserRole.ARTIST, UserStatus.ACTIVE);
    }

    private static ArtField artField(String id, boolean active) {
        ArtField field = new ArtField();
        ReflectionTestUtils.setField(field, "id", UUID.fromString(id));
        field.update("test", "Test", active, (short) 1);
        return field;
    }
}
