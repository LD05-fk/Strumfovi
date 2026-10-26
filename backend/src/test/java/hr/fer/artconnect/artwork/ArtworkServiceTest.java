package hr.fer.artconnect.artwork;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hr.fer.artconnect.artwork.domain.Artwork;
import hr.fer.artconnect.artwork.domain.ArtworkRepository;
import hr.fer.artconnect.artwork.domain.ArtworkVisibility;
import hr.fer.artconnect.artwork.service.ArtworkMapper;
import hr.fer.artconnect.artwork.service.ArtworkService;
import hr.fer.artconnect.artwork.web.ArtworkDto;
import hr.fer.artconnect.artwork.web.ArtworkRequest;
import hr.fer.artconnect.auth.CurrentUserProvider;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import hr.fer.artconnect.support.TestUsers;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArtworkServiceTest {
    private static final UUID ARTIST_ID = UUID.randomUUID();

    @Mock
    ArtworkRepository artworks;

    @Mock
    CurrentUserProvider currentUserProvider;

    @Mock
    ArtworkMapper artworkMapper;

    @Test
    void spremaDjeloNaPrijavljenogUmjetnikaBezSlika() {
        User artist = artist();
        var request = new ArtworkRequest(
                "  Morska tišina  ", 2024, "  Ulje na platnu  ", "  ", "  ", ArtworkVisibility.PUBLIC);
        var expected = mock(ArtworkDto.class);
        when(currentUserProvider.getCurrentUser()).thenReturn(artist);
        when(artworks.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(artworkMapper.toDto(any(), eq(List.of()))).thenReturn(expected);

        var result = service().create(request);

        assertThat(result).isSameAs(expected);
        var saved = ArgumentCaptor.forClass(Artwork.class);
        verify(artworks).save(saved.capture());
        assertThat(saved.getValue().getArtist()).isSameAs(artist);
        assertThat(saved.getValue().getTitle()).isEqualTo("Morska tišina");
        assertThat(saved.getValue().getYear()).isEqualTo((short) 2024);
        assertThat(saved.getValue().getTechnique()).isEqualTo("Ulje na platnu");
        assertThat(saved.getValue().getDimensions()).isNull();
        assertThat(saved.getValue().getDescription()).isNull();
        assertThat(saved.getValue().getVisibility()).isEqualTo(ArtworkVisibility.PUBLIC);
        verify(artworkMapper).toDto(saved.getValue(), List.of());
    }

    @Test
    void bezPrijavljenogKorisnikaNeSpremaDjelo() {
        when(currentUserProvider.getCurrentUser())
                .thenThrow(new ApiException(ErrorCode.UNAUTHENTICATED, "Potrebno je prijaviti se."));

        assertThatThrownBy(() -> service().create(
                new ArtworkRequest("Morska tišina", 2024, "Ulje", null, null, ArtworkVisibility.PRIVATE)))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.UNAUTHENTICATED);
        verify(artworks, never()).save(any());
        verify(artworkMapper, never()).toDto(any(), any());
    }

    private ArtworkService service() {
        return new ArtworkService(artworks, currentUserProvider, artworkMapper);
    }

    private static User artist() {
        return TestUsers.user(ARTIST_ID, "google-umjetnik", "umjetnik@artconnect.test",
                hr.fer.artconnect.auth.domain.UserRole.ARTIST,
                hr.fer.artconnect.auth.domain.UserStatus.ACTIVE);
    }
}
