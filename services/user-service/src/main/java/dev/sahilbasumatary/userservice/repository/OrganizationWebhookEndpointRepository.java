package dev.sahilbasumatary.userservice.repository;

import dev.sahilbasumatary.userservice.entity.OrganizationWebhookEndpoint;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationWebhookEndpointRepository
        extends JpaRepository<OrganizationWebhookEndpoint, UUID> {

    List<OrganizationWebhookEndpoint> findByOrganizationIdAndActiveTrue(UUID organizationId);

    List<OrganizationWebhookEndpoint> findByOrganizationId(UUID organizationId);

    List<OrganizationWebhookEndpoint> findByActiveTrue();
}
