package dev.sahilbasumatary.replayservice.trajectory;

import dev.sahilbasumatary.replayservice.domain.ShotType;
import dev.sahilbasumatary.replayservice.domain.SpinType;
import dev.sahilbasumatary.replayservice.physics.CourtGeometry;

/** Maps a shot type to its physical signature: contact height, dominant spin and required arc. */
public final class ShotKinematics {

    private ShotKinematics() {}

    public static boolean isServe(ShotType shotType) {
        return shotType == ShotType.FIRST_SERVE || shotType == ShotType.SECOND_SERVE;
    }

    public static boolean needsHighArc(ShotType shotType) {
        return shotType == ShotType.LOB || shotType == ShotType.DROP_SHOT;
    }

    public static double contactHeightMetres(ShotType shotType) {
        return switch (shotType) {
            case FIRST_SERVE, SECOND_SERVE, OVERHEAD -> CourtGeometry.SERVE_CONTACT_HEIGHT_METRES;
            case FOREHAND_VOLLEY, BACKHAND_VOLLEY -> CourtGeometry.VOLLEY_CONTACT_HEIGHT_METRES;
            default -> CourtGeometry.GROUNDSTROKE_CONTACT_HEIGHT_METRES;
        };
    }

    public static SpinType spinType(ShotType shotType) {
        return switch (shotType) {
            case FIRST_SERVE,
                    SECOND_SERVE,
                    FOREHAND_GROUNDSTROKE,
                    BACKHAND_GROUNDSTROKE,
                    OVERHEAD ->
                    SpinType.TOPSPIN;
            case FOREHAND_VOLLEY,
                    BACKHAND_VOLLEY,
                    FOREHAND_SLICE,
                    BACKHAND_SLICE,
                    DROP_SHOT,
                    LOB ->
                    SpinType.BACKSPIN;
        };
    }
}
