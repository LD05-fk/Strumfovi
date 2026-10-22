package hr.fer.artconnect.catalog.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ArtFieldRepository extends CatalogRepository<ArtField> {
    @Override
    @Query(value = """
            SELECT EXISTS (SELECT 1 FROM artist_profile_art_fields WHERE art_field_id = :id)
                    OR EXISTS (SELECT 1 FROM opportunity_art_fields WHERE art_field_id = :id)
            """, nativeQuery = true)
    boolean isInUse(@Param("id") UUID id);
}
