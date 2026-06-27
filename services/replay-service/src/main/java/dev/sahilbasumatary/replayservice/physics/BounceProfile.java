package dev.sahilbasumatary.replayservice.physics;

import dev.sahilbasumatary.replayservice.domain.Surface;

/**
 * Surface-specific bounce coefficients.
 *
 * <p>{@code verticalRestitution} is the coefficient of restitution applied to the vertical velocity
 * component (how much bounce height survives the impact). {@code horizontalFriction} scales how
 * aggressively the surface scrubs horizontal pace, and {@code spinForwardFactor} controls how much
 * of the incoming topspin is converted into extra forward pace (the clay "kick").
 */
public record BounceProfile(
        double verticalRestitution, double horizontalFriction, double spinForwardFactor) {

    private static final BounceProfile HARD = new BounceProfile(0.80, 0.60, 0.20);
    private static final BounceProfile CLAY = new BounceProfile(0.85, 0.72, 0.30);
    private static final BounceProfile GRASS = new BounceProfile(0.74, 0.52, 0.12);
    private static final BounceProfile CARPET = new BounceProfile(0.78, 0.50, 0.14);

    public static BounceProfile forSurface(Surface surface) {
        return switch (surface) {
            case HARD -> HARD;
            case CLAY -> CLAY;
            case GRASS -> GRASS;
            case CARPET -> CARPET;
        };
    }
}
