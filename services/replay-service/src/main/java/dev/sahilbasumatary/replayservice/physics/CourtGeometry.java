package dev.sahilbasumatary.replayservice.physics;

/**
 * Regulation court dimensions and ball constants, expressed in SI units (metres, kilograms,
 * seconds). The net sits at {@code y = 0}; positive {@code y} runs towards the receiver's baseline.
 */
public final class CourtGeometry {

    public static final double HALF_LENGTH_METRES = 11.885;
    public static final double SINGLES_HALF_WIDTH_METRES = 4.115;
    public static final double DOUBLES_HALF_WIDTH_METRES = 5.485;
    public static final double SERVICE_LINE_FROM_NET_METRES = 6.40;
    public static final double NET_HEIGHT_CENTRE_METRES = 0.914;
    public static final double NET_HEIGHT_POST_METRES = 1.07;

    public static final double BALL_MASS_KG = 0.057;
    public static final double BALL_RADIUS_METRES = 0.0335;
    public static final double GRAVITY_METRES_PER_SECOND_SQUARED = 9.81;
    public static final double AIR_DENSITY_KG_PER_CUBIC_METRE = 1.21;
    public static final double DRAG_COEFFICIENT = 0.55;

    public static final double GROUNDSTROKE_CONTACT_HEIGHT_METRES = 0.95;
    public static final double VOLLEY_CONTACT_HEIGHT_METRES = 1.15;
    public static final double SERVE_CONTACT_HEIGHT_METRES = 2.65;

    private CourtGeometry() {}

    public static double ballCrossSectionalAreaSquareMetres() {
        return Math.PI * BALL_RADIUS_METRES * BALL_RADIUS_METRES;
    }

    public static double netHeightAt(double lateralX) {
        double clamped = Math.min(Math.abs(lateralX), DOUBLES_HALF_WIDTH_METRES);
        double ratio = clamped / DOUBLES_HALF_WIDTH_METRES;
        return NET_HEIGHT_CENTRE_METRES + (NET_HEIGHT_POST_METRES - NET_HEIGHT_CENTRE_METRES) * ratio;
    }
}
