package hr.fer.artconnect.artwork;

import static org.assertj.core.api.Assertions.assertThat;

import hr.fer.artconnect.artwork.domain.Artwork;
import hr.fer.artconnect.artwork.domain.ArtworkVisibility;
import hr.fer.artconnect.artwork.service.ArtworkVisibilityPolicy;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.support.TestUsers;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ArtworkVisibilityPolicyTest {
    private static final UUID ARTIST_ID = UUID.randomUUID();
    private static final User ACTIVE_ARTIST = user(ARTIST_ID, UserRole.ARTIST, UserStatus.ACTIVE);
    private final ArtworkVisibilityPolicy policy = new ArtworkVisibilityPolicy();

    @Test
    void posjetiteljIliDrugiKorisnikVidiSamoJavnaNeskrivenaDjelaAktivnogUmjetnika() {
        User otherArtist = user(UUID.randomUUID(), UserRole.ARTIST, UserStatus.ACTIVE);

        assertThat(policy.canViewArtwork(artwork(ACTIVE_ARTIST, ArtworkVisibility.PUBLIC, false), null)).isTrue();
        assertThat(policy.canViewArtwork(
                artwork(ACTIVE_ARTIST, ArtworkVisibility.PUBLIC, false), otherArtist)).isTrue();
        assertThat(policy.canViewArtwork(artwork(ACTIVE_ARTIST, ArtworkVisibility.PRIVATE, false), null)).isFalse();
        assertThat(policy.canViewArtwork(artwork(ACTIVE_ARTIST, ArtworkVisibility.PUBLIC, true), null)).isFalse();
    }

    @Test
    void vlasnikIVidiSveSvojeRadoveNeovisnoOVidljivostiIStatusu() {
        for (UserStatus status : new UserStatus[] {UserStatus.ACTIVE, UserStatus.BLOCKED, UserStatus.DELETED}) {
            User artist = user(ARTIST_ID, UserRole.ARTIST, status);
            assertThat(policy.canViewArtist(artist, artist)).isTrue();
            assertThat(policy.canViewArtwork(artwork(artist, ArtworkVisibility.PRIVATE, true), artist)).isTrue();
        }
    }

    @Test
    void administratorVidiSveRadoveIUmjetnike() {
        User admin = user(UUID.randomUUID(), UserRole.ADMIN, UserStatus.ACTIVE);
        for (UserStatus status : new UserStatus[] {UserStatus.ACTIVE, UserStatus.BLOCKED, UserStatus.DELETED}) {
            User artist = user(ARTIST_ID, UserRole.ARTIST, status);
            assertThat(policy.canViewArtist(artist, admin)).isTrue();
            assertThat(policy.canViewAllArtworks(artist, admin)).isTrue();
            assertThat(policy.canViewArtwork(artwork(artist, ArtworkVisibility.PRIVATE, true), admin)).isTrue();
        }
    }

    @Test
    void drugiKorisnikNeVidiPortfolioBlokiranogIliObrisanogUmjetnika() {
        User otherArtist = user(UUID.randomUUID(), UserRole.ARTIST, UserStatus.ACTIVE);
        for (UserStatus status : new UserStatus[] {UserStatus.BLOCKED, UserStatus.DELETED}) {
            User artist = user(ARTIST_ID, UserRole.ARTIST, status);
            assertThat(policy.canViewArtist(artist, null)).isFalse();
            assertThat(policy.canViewArtist(artist, otherArtist)).isFalse();
            assertThat(policy.canViewArtwork(artwork(artist, ArtworkVisibility.PUBLIC, false), otherArtist)).isFalse();
        }
    }

    @Test
    void korisnikKojiNijeUmjetnikNemaJavniPortfolio() {
        User organizer = user(UUID.randomUUID(), UserRole.ORGANIZER, UserStatus.ACTIVE);
        assertThat(policy.canViewArtist(organizer, null)).isFalse();
    }

    private static Artwork artwork(User artist, ArtworkVisibility visibility, boolean hidden) {
        Artwork artwork = Artwork.create(artist, "Rad", (short) 2024, "Ulje", null, null, visibility);
        ReflectionTestUtils.setField(artwork, "hiddenByAdmin", hidden);
        return artwork;
    }

    private static User user(UUID id, UserRole role, UserStatus status) {
        return TestUsers.user(id, "sub-" + id, id + "@test", role, status);
    }
}
