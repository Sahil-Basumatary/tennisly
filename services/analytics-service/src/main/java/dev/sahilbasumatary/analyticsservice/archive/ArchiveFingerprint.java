package dev.sahilbasumatary.analyticsservice.archive;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

public final class ArchiveFingerprint {

    private ArchiveFingerprint() {}

    public static String sha256(List<ArchiveMatchResult> matches) {
        List<ArchiveMatchResult> ordered =
                matches.stream()
                        .sorted(Comparator.comparing(result -> result.matchId().toString()))
                        .toList();
        StringBuilder canonical = new StringBuilder(ordered.size() * 96);
        for (ArchiveMatchResult result : ordered) {
            canonical
                    .append(result.matchId())
                    .append('|')
                    .append(result.accepted())
                    .append('|')
                    .append(result.duplicates())
                    .append('|')
                    .append(result.gaps())
                    .append('|')
                    .append(result.metrics().home().pointsWon())
                    .append('|')
                    .append(result.metrics().away().pointsWon())
                    .append('|')
                    .append(result.metrics().home().servicePointsWon())
                    .append('|')
                    .append(result.metrics().away().servicePointsWon())
                    .append('|')
                    .append(result.metrics().home().breakPointsWon())
                    .append('|')
                    .append(result.metrics().away().breakPointsWon())
                    .append('|')
                    .append(result.metrics().pointsPlayed())
                    .append('\n');
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(
                            digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required for archive fingerprints", ex);
        }
    }
}
