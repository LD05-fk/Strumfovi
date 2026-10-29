package hr.fer.artconnect.artist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import hr.fer.artconnect.artist.domain.ArtistProfile;
import hr.fer.artconnect.artist.domain.ArtistProfileRepository;
import hr.fer.artconnect.artist.domain.PastExhibition;
import hr.fer.artconnect.artist.domain.PastExhibitionRepository;
import hr.fer.artconnect.artist.service.PastExhibitionService;
import hr.fer.artconnect.artist.web.PastExhibitionRequest;
import hr.fer.artconnect.auth.CurrentUserProvider;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import hr.fer.artconnect.common.ApiException;
import hr.fer.artconnect.common.ErrorCode;
import hr.fer.artconnect.support.TestUsers;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PastExhibitionServiceTest {
    private static final UUID ARTIST_ID = UUID.randomUUID();
    private static final UUID EXHIBITION_ID = UUID.randomUUID();

    @Mock
    PastExhibitionRepository pastExhibitions;

    @Mock
    ArtistProfileRepository artistProfiles;

    @Mock
    CurrentUserProvider currentUserProvider;

    @Test
    void spremaIzlozbuNaProfilPrijavljenogUmjetnika() {
        var profile = mock(ArtistProfile.class);
        when(currentUserProvider.getCurrentUser()).thenReturn(artist());
        when(artistProfiles.findById(ARTIST_ID)).thenReturn(Optional.of(profile));
        when(pastExhibitions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var dto = service().create(new PastExhibitionRequest("Gradski motivi", 2024, "Galerija Nova, Zagreb"));

        var saved = ArgumentCaptor.forClass(PastExhibition.class);
        verify(pastExhibitions).save(saved.capture());
        assertThat(saved.getValue().getArtistProfile()).isSameAs(profile);
        assertThat(saved.getValue().getTitle()).isEqualTo("Gradski motivi");
        assertThat(saved.getValue().getYear()).isEqualTo((short) 2024);
        assertThat(saved.getValue().getPlace()).isEqualTo("Galerija Nova, Zagreb");
        assertThat(dto.title()).isEqualTo("Gradski motivi");
        assertThat(dto.year()).isEqualTo(2024);
        assertThat(dto.place()).isEqualTo("Galerija Nova, Zagreb");
        verify(artistProfiles, never()).save(any());
    }

    @Test
    void bezProfilaUmjetnikaStvaraPrazanProfilISpremaIzlozbuNaNjega() {
        when(currentUserProvider.getCurrentUser()).thenReturn(artist());
        when(artistProfiles.findById(ARTIST_ID)).thenReturn(Optional.empty());
        when(artistProfiles.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(pastExhibitions.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service().create(new PastExhibitionRequest("Gradski motivi", 2024, "Zagreb"));

        var savedProfile = ArgumentCaptor.forClass(ArtistProfile.class);
        verify(artistProfiles).save(savedProfile.capture());
        assertThat(savedProfile.getValue().getBiography()).isNull();
        assertThat(savedProfile.getValue().getArtistStatement()).isNull();
        assertThat(savedProfile.getValue().getArtFields()).isEmpty();
        var saved = ArgumentCaptor.forClass(PastExhibition.class);
        verify(pastExhibitions).save(saved.capture());
        assertThat(saved.getValue().getArtistProfile()).isSameAs(savedProfile.getValue());
    }

    @Test
    void neprijavljenomKorisnikuNeDohvacaProfilNiSprema() {
        when(currentUserProvider.getCurrentUser())
                .thenThrow(new ApiException(ErrorCode.UNAUTHENTICATED, "Potrebno je prijaviti se."));

        assertThatThrownBy(() -> service().create(new PastExhibitionRequest("Gradski motivi", 2024, "Zagreb")))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.UNAUTHENTICATED);
        verify(artistProfiles, never()).findById(any());
        verify(pastExhibitions, never()).save(any());
    }

    @Test
    void azuriraVlastituIzlozbu() {
        PastExhibition exhibition = exhibition();
        when(currentUserProvider.getCurrentUser()).thenReturn(artist());
        when(pastExhibitions.findByIdAndArtistProfile_UserId(EXHIBITION_ID, ARTIST_ID))
                .thenReturn(Optional.of(exhibition));

        var dto = service().update(EXHIBITION_ID, new PastExhibitionRequest("Novi naslov", 2024, "Zagreb"));

        assertThat(dto.id()).isEqualTo(EXHIBITION_ID);
        assertThat(dto.title()).isEqualTo("Novi naslov");
        assertThat(dto.year()).isEqualTo(2024);
        assertThat(dto.place()).isEqualTo("Zagreb");
        assertThat(exhibition.getTitle()).isEqualTo("Novi naslov");
        // Promjenu zapisuje praćenje izmjena u transakciji, pa servis ne zove save.
        verify(pastExhibitions, never()).save(any());
    }

    @Test
    void tudjuIzlozbuTretiraKaoNepostojecu() {
        when(currentUserProvider.getCurrentUser()).thenReturn(artist());
        // Vlasništvo provjerava sam upit, pa tuđu izložbu ne vrati ni kad postoji.
        when(pastExhibitions.findByIdAndArtistProfile_UserId(EXHIBITION_ID, ARTIST_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().update(EXHIBITION_ID,
                new PastExhibitionRequest("Novi naslov", 2024, "Zagreb")))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        // Traži se samo među izložbama prijavljenog umjetnika, ne po samom identifikatoru.
        verify(pastExhibitions).findByIdAndArtistProfile_UserId(EXHIBITION_ID, ARTIST_ID);
    }

    @Test
    void nepostojecuIzlozbuPrijavljujeKaoNepostojecu() {
        when(currentUserProvider.getCurrentUser()).thenReturn(artist());
        when(pastExhibitions.findByIdAndArtistProfile_UserId(EXHIBITION_ID, ARTIST_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().update(EXHIBITION_ID,
                new PastExhibitionRequest("Novi naslov", 2024, "Zagreb")))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(pastExhibitions, never()).save(any());
    }

    @Test
    void briseVlastituIzlozbu() {
        PastExhibition exhibition = exhibition();
        when(currentUserProvider.getCurrentUser()).thenReturn(artist());
        when(pastExhibitions.findByIdAndArtistProfile_UserId(EXHIBITION_ID, ARTIST_ID))
                .thenReturn(Optional.of(exhibition));

        service().delete(EXHIBITION_ID);

        verify(pastExhibitions).delete(exhibition);
    }

    @Test
    void izlozbuKojaNijeVlastitaNeBrise() {
        when(currentUserProvider.getCurrentUser()).thenReturn(artist());
        when(pastExhibitions.findByIdAndArtistProfile_UserId(EXHIBITION_ID, ARTIST_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().delete(EXHIBITION_ID))
                .isInstanceOf(ApiException.class)
                .extracting(exception -> ((ApiException) exception).getCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(pastExhibitions, never()).delete(any(PastExhibition.class));
    }

    private PastExhibitionService service() {
        return new PastExhibitionService(pastExhibitions, artistProfiles, currentUserProvider);
    }

    // Pravi entitet, da se vidi što update stvarno mijenja; vlasnika daje mock jer ga postavlja baza.
    private static PastExhibition exhibition() {
        ArtistProfile owner = mock(ArtistProfile.class);
        PastExhibition exhibition = PastExhibition.create(owner, "Stari naslov", (short) 2020, "Split");
        ReflectionTestUtils.setField(exhibition, "id", EXHIBITION_ID);
        return exhibition;
    }

    private static User artist() {
        return TestUsers.user(ARTIST_ID, "google-umjetnik", "umjetnik@artconnect.test",
                UserRole.ARTIST, UserStatus.ACTIVE);
    }
}
