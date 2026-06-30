package dev.sahilbasumatary.replayservice.trajectory;

import dev.sahilbasumatary.replayservice.client.dto.ShotDistributionModel;
import dev.sahilbasumatary.replayservice.domain.PlayerTier;
import dev.sahilbasumatary.replayservice.domain.ShotType;
import dev.sahilbasumatary.replayservice.exception.ReplayGenerationException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory lookup over the shot distributions for one surface, keyed by shot type and player tier.
 * If an exact tier is missing it degrades gracefully through progressively broader tiers so a replay
 * never fails for a sparsely sampled combination.
 */
public final class ShotDistributionIndex {

    private static final List<PlayerTier> TIER_FALLBACK =
            List.of(PlayerTier.TOP_50, PlayerTier.TOP_100, PlayerTier.TOP_10, PlayerTier.OTHER);

    private final Map<ShotType, Map<PlayerTier, ShotDistributionModel>> byShotType;

    private ShotDistributionIndex(Map<ShotType, Map<PlayerTier, ShotDistributionModel>> byShotType) {
        this.byShotType = byShotType;
    }

    public static ShotDistributionIndex from(List<ShotDistributionModel> models) {
        Map<ShotType, Map<PlayerTier, ShotDistributionModel>> index = new EnumMap<>(ShotType.class);
        for (ShotDistributionModel model : models) {
            index.computeIfAbsent(model.shotType(), key -> new EnumMap<>(PlayerTier.class))
                    .put(model.playerTier(), model);
        }
        if (index.isEmpty()) {
            throw new ReplayGenerationException("No shot distributions available for surface");
        }
        return new ShotDistributionIndex(index);
    }

    public ShotDistributionModel resolve(ShotType shotType, PlayerTier tier) {
        Map<PlayerTier, ShotDistributionModel> tiers = byShotType.get(shotType);
        if (tiers == null || tiers.isEmpty()) {
            throw new ReplayGenerationException(
                    "No shot distribution available for shot type " + shotType);
        }
        ShotDistributionModel exact = tiers.get(tier);
        if (exact != null) {
            return exact;
        }
        for (PlayerTier fallback : TIER_FALLBACK) {
            ShotDistributionModel candidate = tiers.get(fallback);
            if (candidate != null) {
                return candidate;
            }
        }
        return tiers.values().iterator().next();
    }
}
