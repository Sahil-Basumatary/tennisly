package dev.sahilbasumatary.userservice.repository;

import dev.sahilbasumatary.userservice.entity.OrganizationMembership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMembershipRepository
        extends JpaRepository<OrganizationMembership, UUID> {

    List<OrganizationMembership> findByOrganizationIdAndActiveTrue(UUID organizationId);

    List<OrganizationMembership> findByUserProfileIdAndActiveTrue(UUID userProfileId);

    Optional<OrganizationMembership> findByUserProfileIdAndOrganizationId(
            UUID userProfileId, UUID organizationId);

    boolean existsByUserProfileIdAndOrganizationId(UUID userProfileId, UUID organizationId);

    long countByOrganizationIdAndActiveTrue(UUID organizationId);
}
