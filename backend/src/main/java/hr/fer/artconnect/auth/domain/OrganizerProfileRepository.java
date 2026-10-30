package hr.fer.artconnect.auth.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface OrganizerProfileRepository extends JpaRepository<OrganizerProfile, UUID> {
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Transactional
    @Query(value = """
            INSERT INTO organizer_profiles (user_id, organization_name, updated_at)
            VALUES (:userId, :organizationName, CURRENT_TIMESTAMP)
            ON CONFLICT (user_id) DO UPDATE
            SET organization_name = EXCLUDED.organization_name,
                updated_at = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    void upsert(@Param("userId") UUID userId, @Param("organizationName") String organizationName);
}
