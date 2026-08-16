package dev.sahilbasumatary.userservice.repository;

import dev.sahilbasumatary.userservice.entity.Organization;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findByClerkOrgId(String clerkOrgId);

    Optional<Organization> findBySlug(String slug);

    @Query(
            """
            SELECT o FROM Organization o
            WHERE (:q IS NULL OR LOWER(o.name) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                OR LOWER(o.slug) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
            AND (:active IS NULL OR o.active = :active)
            """)
    Page<Organization> search(
            @Param("q") String q, @Param("active") Boolean active, Pageable pageable);
}
