package dev.sahilbasumatary.tennisdataservice.repository;

import dev.sahilbasumatary.tennisdataservice.entity.PlayerTier;
import dev.sahilbasumatary.tennisdataservice.entity.ShotDistribution;
import dev.sahilbasumatary.tennisdataservice.entity.ShotType;
import dev.sahilbasumatary.tennisdataservice.entity.Surface;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShotDistributionRepository extends JpaRepository<ShotDistribution, UUID> {

    List<ShotDistribution> findByShotType(ShotType shotType);

    Optional<ShotDistribution> findByShotTypeAndSurfaceAndPlayerTier(
            ShotType shotType, Surface surface, PlayerTier playerTier);

    List<ShotDistribution> findBySurface(Surface surface);

    List<ShotDistribution> findByPlayerTier(PlayerTier playerTier);
}
