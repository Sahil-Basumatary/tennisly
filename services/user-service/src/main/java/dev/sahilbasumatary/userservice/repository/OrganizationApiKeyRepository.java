package dev.sahilbasumatary.userservice.repository;

import dev.sahilbasumatary.userservice.entity.OrganizationApiKey;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganizationApiKeyRepository extends JpaRepository<OrganizationApiKey, UUID> {

    Optional<OrganizationApiKey> findByKeyHash(String keyHash);

    @Query(
            """
            SELECT k FROM OrganizationApiKey k
            WHERE (:organizationId IS NULL OR k.organization.id = :organizationId)
            AND (:active IS NULL OR k.active = :active)
            """)
    Page<OrganizationApiKey> search(
            @Param("organizationId") UUID organizationId,
            @Param("active") Boolean active,
            Pageable pageable);
}
