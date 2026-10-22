package hr.fer.artconnect.catalog.service;

import hr.fer.artconnect.catalog.domain.CatalogEntry;
import hr.fer.artconnect.catalog.domain.CatalogRepository;
import hr.fer.artconnect.catalog.web.CatalogItem;
import hr.fer.artconnect.catalog.web.CatalogItemInput;
import hr.fer.artconnect.common.*;
import java.sql.SQLException;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@Transactional(readOnly = true)
public class CatalogAdminService<T extends CatalogEntry> {
    private final CatalogRepository<T> repository;
    private final Supplier<T> factory;

    public CatalogAdminService(CatalogRepository<T> repository, Supplier<T> factory) {
        this.repository = repository;
        this.factory = factory;
    }

    public PageResponse<CatalogItem> list(PageRequestParams params) {
        var page = repository.findAll(params.toPageable(Sort.by("position", "name")));
        return PageResponse.from(page.map(this::toItem));
    }

    @Transactional
    public CatalogItem create(CatalogItemInput input) {
        if (repository.existsByCodeOrName(input.code(), input.name())) {
            throw duplicate();
        }
        return save(factory.get(), input);
    }

    @Transactional
    public CatalogItem update(UUID id, CatalogItemInput input) {
        T entry = find(id);
        if (repository.existsByCodeAndIdNot(input.code(), id)
                || repository.existsByNameAndIdNot(input.name(), id)) {
            throw duplicate();
        }
        return save(entry, input);
    }

    @Transactional
    public void delete(UUID id) {
        T entry = find(id);
        if (repository.isInUse(id)) {
            throw inUse();
        }
        try {
            repository.delete(entry);
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            // FK štiti i ako netko poveže stavku nakon naše provjere.
            if (hasSqlState(exception, "23503")) {
                throw inUse();
            }
            throw exception;
        }
    }

    private T find(UUID id) {
        return repository.findById(id).orElseThrow(() ->
                new ApiException(ErrorCode.NOT_FOUND, "Stavka šifrarnika nije pronađena."));
    }

    private CatalogItem save(T entry, CatalogItemInput input) {
        entry.update(input.code(), input.name(), input.active(), input.position().shortValue());
        try {
            return toItem(repository.saveAndFlush(entry));
        } catch (DataIntegrityViolationException exception) {
            // UNIQUE štiti i od istovremenih unosa iste šifre ili naziva.
            if (hasSqlState(exception, "23505")) {
                throw duplicate();
            }
            throw exception;
        }
    }

    private CatalogItem toItem(T entry) {
        return new CatalogItem(entry.getId(), entry.getCode(), entry.getName(),
                entry.isActive(), entry.getPosition());
    }

    private static boolean hasSqlState(Throwable exception, String state) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && state.equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    private static ApiException duplicate() {
        return new ApiException(ErrorCode.DUPLICATE_CODE, "Šifra ili naziv već postoji u šifrarniku.");
    }

    private static ApiException inUse() {
        return new ApiException(ErrorCode.IN_USE,
                "Stavka se koristi i ne može se obrisati. Možete je deaktivirati.");
    }
}
