package dev.sahilbasumatary.replayservice.trajectory;

import dev.sahilbasumatary.replayservice.client.dto.ShotDistributionModel;
import dev.sahilbasumatary.replayservice.domain.PlayerTier;
import dev.sahilbasumatary.replayservice.domain.ShotType;
import dev.sahilbasumatary.replayservice.domain.Surface;

/** Test fixtures for shot distribution models. */
final class ShotModels {

    private ShotModels() {}

    static ShotDistributionModel groundstroke(ShotType shotType) {
        return new ShotDistributionModel(
                shotType,
                Surface.HARD,
                PlayerTier.OTHER,
                0.0,
                9.0,
                1.1,
                1.2,
                120.0,
                10.0,
                2800.0,
                400.0,
                1.8,
                0.3,
                5000);
    }

    static ShotDistributionModel serve() {
        return new ShotDistributionModel(
                ShotType.FIRST_SERVE,
                Surface.HARD,
                PlayerTier.OTHER,
                1.0,
                8.35,
                0.7,
                0.9,
                185.0,
                12.0,
                2400.0,
                400.0,
                2.2,
                0.3,
                5000);
    }
}
