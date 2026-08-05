package dev.sahilbasumatary.tennisdataservice.repository;

import dev.sahilbasumatary.tennisdataservice.entity.PlayerProviderRef;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerProviderRefRepository extends JpaRepository<PlayerProviderRef, UUID> {

    Optional<PlayerProviderRef> findByProviderAndProviderRef(String provider, String providerRef);
}
