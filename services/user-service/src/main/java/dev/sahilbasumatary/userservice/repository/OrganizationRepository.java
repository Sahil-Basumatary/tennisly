package dev.sahilbasumatary.userservice.repository;

import dev.sahilbasumatary.userservice.entity.Organization;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findByClerkOrgId(String clerkOrgId);

    Optional<Organization> findBySlug(String slug);
}
