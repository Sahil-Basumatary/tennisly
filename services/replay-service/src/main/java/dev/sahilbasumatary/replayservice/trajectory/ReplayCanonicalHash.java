package dev.sahilbasumatary.replayservice.trajectory;

import dev.sahilbasumatary.replayservice.dto.response.ReplayFrame;
import dev.sahilbasumatary.replayservice.dto.response.ShotSummaryResponse;
import dev.sahilbasumatary.replayservice.physics.Vector3;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

/**
 * Bit-identical fingerprint of assembled replay frames. Coordinates are already rounded to three
 * decimals by {@link FrameAssembler}; this encoding pins that wire format, not a pretty-printer.
 */
public final class ReplayCanonicalHash {

    private ReplayCanonicalHash() {}

    public static String sha256Frames(List<ReplayFrame> frames) {
        return sha256(encodeFrames(frames));
    }

    public static String sha256Shots(List<ShotSummaryResponse> shots) {
        return sha256(encodeShots(shots));
    }

    public static String encodeFrames(List<ReplayFrame> frames) {
        StringBuilder out = new StringBuilder(frames.size() * 96);
        for (ReplayFrame frame : frames) {
            out.append(fmt(frame.timeSeconds())).append('|');
            appendVector(out, frame.ball());
            out.append('|');
            appendVector(out, frame.home());
            out.append('|');
            appendVector(out, frame.away());
            out.append('|')
                    .append(frame.pointSequence())
                    .append('|')
                    .append(frame.shotIndex())
                    .append('|')
                    .append(frame.shotType())
                    .append('\n');
        }
        return out.toString();
    }

    public static String encodeShots(List<ShotSummaryResponse> shots) {
        StringBuilder out = new StringBuilder(shots.size() * 96);
        for (ShotSummaryResponse shot : shots) {
            out.append(shot.pointSequence())
                    .append('|')
                    .append(shot.shotIndex())
                    .append('|')
                    .append(shot.shotType())
                    .append('|')
                    .append(shot.hitter())
                    .append('|')
                    .append(shot.spin())
                    .append('|');
            appendVector(out, shot.contact());
            out.append('|');
            appendVector(out, shot.landing());
            out.append('|')
                    .append(fmt(shot.launchSpeedKmh()))
                    .append('|')
                    .append(fmt(shot.apexHeightMetres()))
                    .append('|')
                    .append(fmt(shot.flightSeconds()))
                    .append('\n');
        }
        return out.toString();
    }

    public static String sha256(String canonical) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required for replay golden hashes", ex);
        }
    }

    private static void appendVector(StringBuilder out, Vector3 vector) {
        out.append(fmt(vector.x()))
                .append(',')
                .append(fmt(vector.y()))
                .append(',')
                .append(fmt(vector.z()));
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
