package dev.sahilbasumatary.tennisdataservice.dto.response;

import dev.sahilbasumatary.tennisdataservice.entity.PlayerTier;
import dev.sahilbasumatary.tennisdataservice.entity.ShotDistribution;
import dev.sahilbasumatary.tennisdataservice.entity.ShotType;
import dev.sahilbasumatary.tennisdataservice.entity.Surface;
import java.util.UUID;

public record ShotDistributionResponse(
        UUID id,
        ShotType shotType,
        Surface surface,
        PlayerTier playerTier,
        Double meanLandingX,
        Double meanLandingY,
        Double stdDevX,
        Double stdDevY,
        Double meanSpeedKmh,
        Double speedStdDev,
        Double meanSpinRpm,
        Double spinStdDev,
        Double meanArcHeight,
        Double arcStdDev,
        Integer sampleSize) {

    public static ShotDistributionResponse from(ShotDistribution distribution) {
        return new ShotDistributionResponse(
                distribution.getId(),
                distribution.getShotType(),
                distribution.getSurface(),
                distribution.getPlayerTier(),
                distribution.getMeanLandingX(),
                distribution.getMeanLandingY(),
                distribution.getStdDevX(),
                distribution.getStdDevY(),
                distribution.getMeanSpeedKmh(),
                distribution.getSpeedStdDev(),
                distribution.getMeanSpinRpm(),
                distribution.getSpinStdDev(),
                distribution.getMeanArcHeight(),
                distribution.getArcStdDev(),
                distribution.getSampleSize());
    }
}
