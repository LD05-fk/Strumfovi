package hr.fer.artconnect.catalog.domain;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtFieldRepository extends JpaRepository<ArtField, UUID> {
    Page<ArtField> findAllByActiveTrue(Pageable pageable);
}
