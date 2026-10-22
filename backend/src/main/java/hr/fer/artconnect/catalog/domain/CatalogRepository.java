package hr.fer.artconnect.catalog.domain;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface CatalogRepository<T extends CatalogEntry> extends JpaRepository<T, UUID> {
    Page<T> findAllByActiveTrue(Pageable pageable);

    boolean existsByCodeOrName(String code, String name);

    boolean existsByCodeAndIdNot(String code, UUID id);

    boolean existsByNameAndIdNot(String name, UUID id);

    boolean isInUse(UUID id);
}
