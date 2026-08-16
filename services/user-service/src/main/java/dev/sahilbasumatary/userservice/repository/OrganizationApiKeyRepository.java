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
            value =
                    """
                    SELECT k FROM OrganizationApiKey k
                    JOIN FETCH k.organization
                    WHERE (CAST(:organizationId AS uuid) IS NULL
                        OR k.organization.id = CAST(:organizationId AS uuid))
                    AND (CAST(:active AS boolean) IS NULL OR k.active = CAST(:active AS boolean))
                    """,
            countQuery =
                    """
                    SELECT COUNT(k) FROM OrganizationApiKey k
                    WHERE (CAST(:organizationId AS uuid) IS NULL
                        OR k.organization.id = CAST(:organizationId AS uuid))
                    AND (CAST(:active AS boolean) IS NULL OR k.active = CAST(:active AS boolean))
                    """)
    Page<OrganizationApiKey> search(
            @Param("organizationId") UUID organizationId,
            @Param("active") Boolean active,
            Pageable pageable);
}
