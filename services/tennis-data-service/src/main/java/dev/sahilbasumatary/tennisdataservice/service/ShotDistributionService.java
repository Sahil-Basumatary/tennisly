package dev.sahilbasumatary.tennisdataservice.service;

import dev.sahilbasumatary.tennisdataservice.config.RedisCacheConfig;
import dev.sahilbasumatary.tennisdataservice.dto.response.ShotDistributionResponse;
import dev.sahilbasumatary.tennisdataservice.entity.PlayerTier;
import dev.sahilbasumatary.tennisdataservice.entity.ShotDistribution;
import dev.sahilbasumatary.tennisdataservice.entity.ShotType;
import dev.sahilbasumatary.tennisdataservice.entity.Surface;
import dev.sahilbasumatary.tennisdataservice.exception.ResourceNotFoundException;
import dev.sahilbasumatary.tennisdataservice.repository.ShotDistributionRepository;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShotDistributionService {

    private final ShotDistributionRepository shotDistributionRepository;

    public ShotDistributionService(ShotDistributionRepository shotDistributionRepository) {
        this.shotDistributionRepository = shotDistributionRepository;
    }

    @Cacheable(
            cacheNames = RedisCacheConfig.SHOT_DISTRIBUTIONS_CACHE,
            key =
                    "(#shotType != null ? #shotType.name() : 'ALL') + ':' + (#surface != null ?"
                            + " #surface.name() : 'ALL') + ':' + (#playerTier != null ?"
                            + " #playerTier.name() : 'ALL')")
    @Transactional(readOnly = true)
    public List<ShotDistributionResponse> listDistributions(
            ShotType shotType, Surface surface, PlayerTier playerTier) {
        List<ShotDistribution> distributions;
        if (shotType != null) {
            distributions = shotDistributionRepository.findByShotType(shotType);
        } else if (surface != null) {
            distributions = shotDistributionRepository.findBySurface(surface);
        } else if (playerTier != null) {
            distributions = shotDistributionRepository.findByPlayerTier(playerTier);
        } else {
            distributions = shotDistributionRepository.findAll();
        }
        return distributions.stream().map(ShotDistributionResponse::from).toList();
    }

    @Cacheable(
            cacheNames = RedisCacheConfig.SHOT_DISTRIBUTIONS_CACHE,
            key = "'type:' + #shotType.name()")
    @Transactional(readOnly = true)
    public List<ShotDistributionResponse> getByShotType(ShotType shotType) {
        List<ShotDistributionResponse> results =
                shotDistributionRepository.findByShotType(shotType).stream()
                        .map(ShotDistributionResponse::from)
                        .toList();
        if (results.isEmpty()) {
            throw new ResourceNotFoundException("ShotDistribution", shotType);
        }
        return results;
    }
}
