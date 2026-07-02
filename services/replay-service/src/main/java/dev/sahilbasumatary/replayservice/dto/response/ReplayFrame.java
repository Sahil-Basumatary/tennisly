package dev.sahilbasumatary.replayservice.dto.response;

import dev.sahilbasumatary.replayservice.domain.ShotType;
import dev.sahilbasumatary.replayservice.physics.Vector3;

/**
 * A single rendered frame: the ball and both players in court coordinates (metres) at a wall-clock
 * offset from the start of the replay.
 */
public record ReplayFrame(
        double timeSeconds,
        Vector3 ball,
        Vector3 home,
        Vector3 away,
        int pointSequence,
        int shotIndex,
        ShotType shotType) {}
