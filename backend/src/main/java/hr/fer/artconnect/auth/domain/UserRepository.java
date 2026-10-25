package hr.fer.artconnect.auth.domain;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByGoogleSub(String googleSub);

    Optional<User> findByEmailIgnoreCase(String email);

    long countByRoleAndStatus(UserRole role, UserStatus status);

    // Uvjet u bazi osigurava da i dva istodobna zahtjeva mogu odabrati ulogu samo jednom.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE User u SET u.role = :role, u.status = hr.fer.artconnect.auth.domain.UserStatus.ACTIVE,
                u.organizerApproval = :approval, u.updatedAt = :updatedAt
            WHERE u.id = :id AND u.status = hr.fer.artconnect.auth.domain.UserStatus.ONBOARDING
            """)
    int chooseRole(UUID id, UserRole role, OrganizerApproval approval, OffsetDateTime updatedAt);
}
