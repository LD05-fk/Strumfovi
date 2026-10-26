package hr.fer.artconnect.opportunity.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpportunityRepository extends JpaRepository<Opportunity, UUID> {
}
