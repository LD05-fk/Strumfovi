package hr.fer.artconnect.admin.domain;

import hr.fer.artconnect.auth.domain.OrganizerApproval;
import hr.fer.artconnect.auth.domain.User;
import hr.fer.artconnect.auth.domain.UserRole;
import hr.fer.artconnect.auth.domain.UserStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface AdminUserRepository extends Repository<User, UUID> {
    // LEFT JOIN zadržava korisnike bez profila organizatora.
    // LOCATE traži doslovni dio teksta, pa % i _ nisu posebni znakovi.
    // CAST određuje tekstualni tip parametra i kad je q null.
    @Query(value = """
            SELECT u AS user, profile.organizationName AS organizationName
            FROM User u
            LEFT JOIN OrganizerProfile profile ON profile.userId = u.id
            WHERE (:role IS NULL OR u.role = :role)
              AND (:status IS NULL OR u.status = :status)
              AND (:organizerApproval IS NULL OR u.organizerApproval = :organizerApproval)
              AND (:q IS NULL OR LOCATE(LOWER(CAST(:q AS string)), LOWER(u.displayName)) > 0
                   OR LOCATE(LOWER(CAST(:q AS string)), LOWER(u.email)) > 0)
            ORDER BY u.createdAt DESC, u.id DESC
            """, countQuery = """
            SELECT COUNT(u)
            FROM User u
            WHERE (:role IS NULL OR u.role = :role)
              AND (:status IS NULL OR u.status = :status)
              AND (:organizerApproval IS NULL OR u.organizerApproval = :organizerApproval)
              AND (:q IS NULL OR LOCATE(LOWER(CAST(:q AS string)), LOWER(u.displayName)) > 0
                   OR LOCATE(LOWER(CAST(:q AS string)), LOWER(u.email)) > 0)
            """)
    Page<UserWithOrganization> findUsers(
            @Param("q") String q,
            @Param("role") UserRole role,
            @Param("status") UserStatus status,
            @Param("organizerApproval") OrganizerApproval organizerApproval,
            Pageable pageable);

    interface UserWithOrganization {
        User getUser();

        String getOrganizationName();
    }
}
