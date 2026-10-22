package hr.fer.artconnect.catalog;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import hr.fer.artconnect.catalog.domain.ArtField;
import hr.fer.artconnect.catalog.domain.ArtFieldRepository;
import hr.fer.artconnect.catalog.service.CatalogAdminService;
import hr.fer.artconnect.catalog.web.CatalogItemInput;
import hr.fer.artconnect.common.*;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

class CatalogAdminServiceTest {
    private final ArtFieldRepository repository = mock(ArtFieldRepository.class);
    private final CatalogAdminService<ArtField> service = new CatalogAdminService<>(repository, ArtField::new);
    private final CatalogItemInput input = new CatalogItemInput("keramika", "Keramika", false, 12);
    private final UUID id = UUID.randomUUID();

    @BeforeEach
    void pripremiSpremanje() {
        lenient().when(repository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void listaPretvaraStranicenjeSortiraIMapiraStavke() {
        ArtField entry = new ArtField();
        entry.update("keramika", "Keramika", false, (short) 12);
        when(repository.findAll(any(Pageable.class))).thenAnswer(call ->
                new PageImpl<>(List.of(entry), call.getArgument(0), 3));
        PageRequestParams params = new PageRequestParams();
        params.setPage(2);
        params.setPageSize(2);

        var result = service.list(params);
        assertThat(result.data()).hasSize(1);
        assertThat(result.data().getFirst().active()).isFalse();
        assertThat(result.data().getFirst().position()).isEqualTo((short) 12);
        verify(repository).findAll(argThat((Pageable page) -> page.getPageNumber() == 1
                && page.getPageSize() == 2
                && page.getSort().toString().equals("position: ASC,name: ASC")));
    }

    @Test
    void dodavanjeIPromjenaMapirajuSvaPolja() {
        var created = service.create(input);
        assertThat(created.code()).isEqualTo("keramika");
        assertThat(created.name()).isEqualTo("Keramika");
        assertThat(created.active()).isFalse();
        assertThat(created.position()).isEqualTo((short) 12);

        ArtField entry = new ArtField();
        when(repository.findById(id)).thenReturn(Optional.of(entry));
        service.update(id, input);
        assertThat(entry.getName()).isEqualTo("Keramika");
        verify(repository).existsByCodeAndIdNot("keramika", id);
        verify(repository).existsByNameAndIdNot("Keramika", id);
    }

    @Test
    void odbijaDuplikatPrijeSpremanja() {
        when(repository.existsByCodeOrName(anyString(), anyString())).thenReturn(true);
        assertCode(() -> service.create(input), ErrorCode.DUPLICATE_CODE);
        verify(repository, never()).saveAndFlush(any());

        when(repository.findById(id)).thenReturn(Optional.of(new ArtField()));
        when(repository.existsByNameAndIdNot(anyString(), eq(id))).thenReturn(true);
        assertCode(() -> service.update(id, input), ErrorCode.DUPLICATE_CODE);
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void nepostojecaStavkaVracaNotFound() {
        assertCode(() -> service.update(id, input), ErrorCode.NOT_FOUND);
        assertCode(() -> service.delete(id), ErrorCode.NOT_FOUND);
        verify(repository, never()).delete(any());
    }

    @Test
    void briseSamoNekoristenuStavku() {
        ArtField entry = new ArtField();
        when(repository.findById(id)).thenReturn(Optional.of(entry));
        when(repository.isInUse(id)).thenReturn(true);
        assertCode(() -> service.delete(id), ErrorCode.IN_USE);
        verify(repository, never()).delete(any());

        when(repository.isInUse(id)).thenReturn(false);
        service.delete(id);
        verify(repository).delete(entry);
        verify(repository).flush();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void uniqueGreskuPriIstovremenomSpremanjuPrevodiUDuplicateCode(boolean update) {
        when(repository.findById(id)).thenReturn(Optional.of(new ArtField()));
        when(repository.saveAndFlush(any())).thenThrow(constraint("23505"));
        assertCode(() -> {
            if (update) {
                service.update(id, input);
            } else {
                service.create(input);
            }
        }, ErrorCode.DUPLICATE_CODE);
    }

    @Test
    void fkGreskuNakonProvjereKoristenjaPrevodiUInUse() {
        when(repository.findById(id)).thenReturn(Optional.of(new ArtField()));
        doThrow(constraint("23503")).when(repository).flush();
        assertCode(() -> service.delete(id), ErrorCode.IN_USE);
    }

    @Test
    void ostaleGreskeBazeNePrikazujeKaoDuplikatIliKoristenje() {
        var failure = constraint("23514");
        when(repository.saveAndFlush(any())).thenThrow(failure);
        assertThatThrownBy(() -> service.create(input)).isSameAs(failure);
        when(repository.findById(id)).thenReturn(Optional.of(new ArtField()));
        doThrow(failure).when(repository).flush();
        assertThatThrownBy(() -> service.delete(id)).isSameAs(failure);
    }

    private static DataIntegrityViolationException constraint(String state) {
        return new DataIntegrityViolationException("test", new SQLException("test", state));
    }

    private static void assertCode(ThrowingCallable action, ErrorCode code) {
        assertThatThrownBy(action).isInstanceOfSatisfying(ApiException.class,
                exception -> assertThat(exception.getCode()).isEqualTo(code));
    }
}
