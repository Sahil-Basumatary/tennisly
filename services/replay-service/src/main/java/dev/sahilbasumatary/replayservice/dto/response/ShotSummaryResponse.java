package dev.sahilbasumatary.replayservice.dto.response;

import dev.sahilbasumatary.replayservice.domain.PlayerSide;
import dev.sahilbasumatary.replayservice.domain.ShotType;
import dev.sahilbasumatary.replayservice.domain.SpinType;
import dev.sahilbasumatary.replayservice.physics.Vector3;

/** Per-shot analytics overlay (placement, pace and arc) that accompanies the rendered frames. */
public record ShotSummaryResponse(
        int pointSequence,
        int shotIndex,
        ShotType shotType,
        PlayerSide hitter,
        SpinType spin,
        Vector3 contact,
        Vector3 landing,
        double launchSpeedKmh,
        double apexHeightMetres,
        double flightSeconds) {}
