package hr.fer.artconnect.artist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import hr.fer.artconnect.artist.domain.ArtistLinkRepository;
import hr.fer.artconnect.artist.domain.ArtistProfileRepository;
import hr.fer.artconnect.artist.domain.PastExhibition;
import hr.fer.artconnect.artist.domain.PastExhibitionRepository;
import hr.fer.artconnect.artist.service.ArtistParticipationService;
import hr.fer.artconnect.artist.service.ArtistPublicProfileService;
import hr.fer.artconnect.auth.CurrentUserProvider;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRepository;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import hr.fer.artconnect.support.TestUsers;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArtistPublicProfileServiceTest {
    private static final UUID ARTIST_ID = UUID.randomUUID();

    @Mock
    UserRepository users;

    @Mock
    ArtistProfileRepository artistProfiles;

    @Mock
    ArtistLinkRepository artistLinks;

    @Mock
    PastExhibitionRepository pastExhibitions;

    @Mock
    CurrentUserProvider currentUserProvider;

    @Test
    void vraćaPrazanProfilISudjelovanjaZaJavnogUmjetnika() {
        User artist = user(ARTIST_ID, UserRole.ARTIST, UserStatus.ACTIVE);
        when(users.findById(ARTIST_ID)).thenReturn(Optional.of(artist));
        when(currentUserProvider.getOptionalCurrentUser()).thenReturn(Optional.empty());
        when(artistProfiles.findById(ARTIST_ID)).thenReturn(Optional.empty());
        when(pastExhibitions.findAllByArtistProfile_UserId(ARTIST_ID)).thenReturn(List.of());

        var result = service().getPublicProfile(ARTIST_ID);

        assertThat(result.userId()).isEqualTo(ARTIST_ID);
        assertThat(result.displayName()).isEqualTo("Test korisnik");
        assertThat(result.biography()).isNull();
        assertThat(result.artFields()).isEmpty();
        assertThat(result.links()).isEmpty();
        assertThat(result.pastExhibitions()).isEmpty();
        assertThat(result.participations()).isEmpty();
    }

    @Test
    void skrivaNeumjetnikeINeispravneStatuseOdJavnosti() {
        for (UserStatus status : List.of(
                UserStatus.BLOCKED, UserStatus.DELETED, UserStatus.ONBOARDING, UserStatus.INVITED)) {
            UUID hiddenArtistId = UUID.randomUUID();
            User artist = user(hiddenArtistId, UserRole.ARTIST, status);
            when(users.findById(hiddenArtistId)).thenReturn(Optional.of(artist));
            when(currentUserProvider.getOptionalCurrentUser()).thenReturn(Optional.empty());

            assertNotFound(hiddenArtistId);
        }

        UUID organizerId = UUID.randomUUID();
        when(users.findById(organizerId))
                .thenReturn(Optional.of(user(organizerId, UserRole.ORGANIZER, UserStatus.ACTIVE)));
        assertNotFound(organizerId);
        UUID unknownId = UUID.randomUUID();
        when(users.findById(unknownId)).thenReturn(Optional.empty());
        assertNotFound(unknownId);
    }

    @Test
    void vlasnikIAdministratorMoguVidjetiBlokiranogUmjetnika() {
        User blockedArtist = user(ARTIST_ID, UserRole.ARTIST, UserStatus.BLOCKED);
        when(users.findById(ARTIST_ID)).thenReturn(Optional.of(blockedArtist));
        when(artistProfiles.findById(ARTIST_ID)).thenReturn(Optional.empty());
        when(pastExhibitions.findAllByArtistProfile_UserId(ARTIST_ID)).thenReturn(List.of());

        when(currentUserProvider.getOptionalCurrentUser()).thenReturn(Optional.of(blockedArtist));
        assertThat(service().getPublicProfile(ARTIST_ID).userId()).isEqualTo(ARTIST_ID);

        when(currentUserProvider.getOptionalCurrentUser())
                .thenReturn(Optional.of(user(UUID.randomUUID(), UserRole.ADMIN, UserStatus.ACTIVE)));
        assertThat(service().getPublicProfile(ARTIST_ID).userId()).isEqualTo(ARTIST_ID);
    }

    @Test
    void prethodneIzložbeSortiraPoGodiniIPoDatumuStvaranjaSilazno() {
        User artist = user(ARTIST_ID, UserRole.ARTIST, UserStatus.ACTIVE);
        var earlierYear = exhibition("ranija godina", 2023, "2025-01-01T10:00:00Z");
        var laterSameYear = exhibition("kasnija godina", 2023, "2025-01-02T10:00:00Z");
        var newerYear = exhibition("novija godina", 2024, "2025-01-01T10:00:00Z");
        when(users.findById(ARTIST_ID)).thenReturn(Optional.of(artist));
        when(currentUserProvider.getOptionalCurrentUser()).thenReturn(Optional.empty());
        when(artistProfiles.findById(ARTIST_ID)).thenReturn(Optional.empty());
        when(pastExhibitions.findAllByArtistProfile_UserId(ARTIST_ID))
                .thenReturn(List.of(newerYear, earlierYear, laterSameYear));

        var result = service().getPublicProfile(ARTIST_ID);

        assertThat(result.pastExhibitions())
                .extracting(exhibition -> exhibition.title())
                .containsExactly("novija godina", "kasnija godina", "ranija godina");
    }

    private void assertNotFound(UUID artistId) {
        assertThatThrownBy(() -> service().getPublicProfile(artistId))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    private ArtistPublicProfileService service() {
        return new ArtistPublicProfileService(
                users, artistProfiles, artistLinks, pastExhibitions,
                new ArtistParticipationService(), currentUserProvider);
    }

    private static User user(UUID id, UserRole role, UserStatus status) {
        return TestUsers.user(id, "google-" + id, id + "@artconnect.test", role, status);
    }

    private static PastExhibition exhibition(String title, int year, String createdAt) {
        PastExhibition exhibition = mock(PastExhibition.class);
        when(exhibition.getId()).thenReturn(UUID.randomUUID());
        when(exhibition.getTitle()).thenReturn(title);
        when(exhibition.getYear()).thenReturn((short) year);
        when(exhibition.getPlace()).thenReturn("Zagreb");
        lenient().when(exhibition.getCreatedAt()).thenReturn(OffsetDateTime.parse(createdAt));
        return exhibition;
    }
}
