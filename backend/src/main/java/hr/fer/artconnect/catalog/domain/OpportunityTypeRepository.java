package hr.fer.artconnect.catalog.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpportunityTypeRepository extends CatalogRepository<OpportunityType> {
    @Override
    @Query(value = """
            SELECT EXISTS (SELECT 1 FROM opportunities WHERE type_id = :id)
            """, nativeQuery = true)
    boolean isInUse(@Param("id") UUID id);
}
