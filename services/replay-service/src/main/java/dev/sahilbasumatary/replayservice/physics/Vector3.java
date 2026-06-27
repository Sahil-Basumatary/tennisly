package dev.sahilbasumatary.replayservice.physics;

/**
 * Immutable 3D vector used for positions, velocities and forces in the trajectory engine.
 *
 * <p>The court frame is right-handed: {@code x} is lateral (sideline to sideline), {@code y} is
 * depth (net to baseline) and {@code z} is height above the ground in metres.
 */
public record Vector3(double x, double y, double z) {

    public static final Vector3 ZERO = new Vector3(0, 0, 0);

    public static Vector3 of(double x, double y, double z) {
        return new Vector3(x, y, z);
    }

    public Vector3 add(Vector3 other) {
        return new Vector3(x + other.x, y + other.y, z + other.z);
    }

    public Vector3 subtract(Vector3 other) {
        return new Vector3(x - other.x, y - other.y, z - other.z);
    }

    public Vector3 scale(double factor) {
        return new Vector3(x * factor, y * factor, z * factor);
    }

    public Vector3 cross(Vector3 other) {
        return new Vector3(
                y * other.z - z * other.y,
                z * other.x - x * other.z,
                x * other.y - y * other.x);
    }

    public double dot(Vector3 other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public double magnitude() {
        return Math.sqrt(x * x + y * y + z * z);
    }

    public double horizontalMagnitude() {
        return Math.sqrt(x * x + y * y);
    }

    public Vector3 normalized() {
        double magnitude = magnitude();
        if (magnitude == 0.0) {
            return ZERO;
        }
        return scale(1.0 / magnitude);
    }
}
