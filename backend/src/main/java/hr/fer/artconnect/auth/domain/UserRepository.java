package hr.fer.artconnect.auth.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByGoogleSub(String googleSub);

    Optional<User> findByEmailIgnoreCase(String email);

    long countByRoleAndStatus(UserRole role, UserStatus status);
}
