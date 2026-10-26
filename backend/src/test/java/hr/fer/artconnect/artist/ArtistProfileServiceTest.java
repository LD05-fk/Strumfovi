package hr.fer.artconnect.artist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import hr.fer.artconnect.auth.CurrentUserProvider;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.catalog.domain.ArtField;
import hr.fer.artconnect.catalog.domain.ArtFieldRepository;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import hr.fer.artconnect.support.TestUsers;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArtistProfileServiceTest {
    private static final UUID ARTIST_ID = UUID.randomUUID();

    @Mock
    ArtistProfileRepository artistProfiles;

    @Mock
    ArtistLinkRepository artistLinks;

    @Mock
    ArtFieldRepository artFields;

    @Mock
    CurrentUserProvider currentUserProvider;

    @Test
    void vracaPrazanProfilBezStvaranjaReda() {
        User user = artist(UserStatus.ACTIVE);
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(artistProfiles.findById(ARTIST_ID)).thenReturn(Optional.empty());

        var dto = service().getMyArtistProfile();

        assertThat(dto.userId()).isEqualTo(ARTIST_ID);
        assertThat(dto.displayName()).isEqualTo("Test korisnik");
        assertThat(dto.avatarUrl()).isNull();
        assertThat(dto.biography()).isNull();
        assertThat(dto.artistStatement()).isNull();
        assertThat(dto.artFields()).isEmpty();
        assertThat(dto.links()).isEmpty();
        verify(artistLinks, never()).findAllForArtistProfile(ARTIST_ID);
        verify(artistProfiles, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void vracaPodrucjaIPoveznicePoredanePoPozicijiIPoIdKodJednakePozicije() {
        User user = artist(UserStatus.ACTIVE);
        ArtistProfile profile = mock(ArtistProfile.class);
        var fieldFirst = artField("00000000-0000-0000-0000-000000000001", (short) 1, "prvo");
        var fieldSecond = artField("00000000-0000-0000-0000-000000000002", (short) 1, "drugo");
        var fieldHighId = artField("80000000-0000-0000-0000-000000000001", (short) 1, "visoki-id");
        var fieldThird = artField("00000000-0000-0000-0000-000000000003", (short) 2, "trece");
        var links = List.of(
                link("00000000-0000-0000-0000-000000000001", (short) 1, "Prva"),
                link("00000000-0000-0000-0000-000000000002", (short) 1, "Druga"),
                link("00000000-0000-0000-0000-000000000003", (short) 2, "Treća"));
        when(profile.getBiography()).thenReturn("Bio");
        when(profile.getArtistStatement()).thenReturn("Izjava");
        when(profile.getArtFields())
                .thenReturn(new LinkedHashSet<>(List.of(fieldThird, fieldHighId, fieldSecond, fieldFirst)));
        when(currentUserProvider.getCurrentUser()).thenReturn(user);
        when(artistProfiles.findById(ARTIST_ID)).thenReturn(Optional.of(profile));
        when(artistLinks.findAllForArtistProfile(ARTIST_ID)).thenReturn(links);

        var dto = service().getMyArtistProfile();

        assertThat(dto.biography()).isEqualTo("Bio");
        assertThat(dto.artistStatement()).isEqualTo("Izjava");
        assertThat(dto.artFields()).map(ArtistProfileDto.CatalogRef::code)
                .containsExactly("prvo", "drugo", "visoki-id", "trece");
        assertThat(dto.links()).map(ArtistProfileDto.ArtistLink::label).containsExactly("Prva", "Druga", "Treća");
        verify(artistLinks).findAllForArtistProfile(ARTIST_ID);
    }

    @Test
    void blokiranomKorisnikuVracaUserBlockedBezDohvataProfila() {
        when(currentUserProvider.getCurrentUser()).thenReturn(artist(UserStatus.BLOCKED));

        assertThatThrownBy(() -> service().getMyArtistProfile())
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.USER_BLOCKED));

        verify(artistProfiles, never()).findById(ARTIST_ID);
        verify(artistLinks, never()).findAllForArtistProfile(ARTIST_ID);
    }

    private ArtistProfileService service() {
        return new ArtistProfileService(artistProfiles, artistLinks, artFields, currentUserProvider);
    }

    private static User artist(UserStatus status) {
        return TestUsers.user(ARTIST_ID, "google-umjetnik", "umjetnik@artconnect.test",
                UserRole.ARTIST, status);
    }

    private static ArtField artField(String id, short position, String code) {
        var field = new ArtField();
        ReflectionTestUtils.setField(field, "id", UUID.fromString(id));
        field.update(code, code, true, position);
        return field;
    }

    private static ArtistLink link(String id, short position, String label) {
        return new TestArtistLink(UUID.fromString(id), position, label);
    }

    private static final class TestArtistLink extends ArtistLink {
        private final UUID id;
        private final short position;
        private final String label;

        private TestArtistLink(UUID id, short position, String label) {
            this.id = id;
            this.position = position;
            this.label = label;
        }

        @Override
        public UUID getId() {
            return id;
        }

        @Override
        public short getPosition() {
            return position;
        }

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public String getUrl() {
            return "https://example.test/" + id;
        }
    }
}
