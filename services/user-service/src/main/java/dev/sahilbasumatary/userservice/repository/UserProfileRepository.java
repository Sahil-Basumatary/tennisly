package dev.sahilbasumatary.userservice.repository;

import dev.sahilbasumatary.userservice.entity.UserProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByClerkId(String clerkId);

    Optional<UserProfile> findByEmail(String email);

    boolean existsByClerkId(String clerkId);

    @Query(
            """
            SELECT u FROM UserProfile u
            WHERE (:q IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                OR LOWER(u.displayName) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                OR LOWER(u.clerkId) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
            AND (:active IS NULL OR u.active = :active)
            """)
    Page<UserProfile> search(
            @Param("q") String q, @Param("active") Boolean active, Pageable pageable);
}
