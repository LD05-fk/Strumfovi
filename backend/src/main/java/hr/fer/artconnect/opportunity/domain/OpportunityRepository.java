package hr.fer.artconnect.opportunity.domain;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpportunityRepository extends JpaRepository<Opportunity, UUID> {
    @EntityGraph(attributePaths = "type")
    @Query(
            value = """
                    SELECT opportunity FROM Opportunity opportunity
                    WHERE opportunity.organizer.id = :organizerId
                      AND opportunity.archived = :archived
                      AND (:status IS NULL OR opportunity.status = :status)
                    """,
            countQuery = """
                    SELECT COUNT(opportunity) FROM Opportunity opportunity
                    WHERE opportunity.organizer.id = :organizerId
                      AND opportunity.archived = :archived
                      AND (:status IS NULL OR opportunity.status = :status)
                    """)
    Page<Opportunity> findOrganizerOpportunities(@Param("organizerId") UUID organizerId,
            @Param("archived") boolean archived, @Param("status") OpportunityStatus status, Pageable pageable);
}
