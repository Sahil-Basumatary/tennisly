package dev.sahilbasumatary.matchservice.service;

import java.util.UUID;

/** Deterministic TSV rows for archive COPY. JSON payloads are production-sized, not empty objects. */
public final class ArchiveStagingTsv {

    private ArchiveStagingTsv() {}

    public static String productionRow(
            UUID jobId, UUID matchId, UUID home, UUID away, int sequence, long seed) {
        boolean awayWon = ((seed + sequence) & 1L) == 0L;
        UUID server = sequence % 2 == 1 ? home : away;
        UUID winner = awayWon ? away : home;
        int set = 1 + (sequence / 180);
        int game = 1 + (sequence % 12);
        String score =
                "{\"set\":"
                        + set
                        + ",\"game\":"
                        + game
                        + ",\"points\":[\"30\",\"15\"],\"server\":\""
                        + server
                        + "\",\"n\":"
                        + sequence
                        + "}";
        String shots =
                "{\"shots\":["
                        + "{\"type\":\"FIRST_SERVE\",\"speed\":48.2,\"spin\":2200},"
                        + "{\"type\":\"FOREHAND_GROUNDSTROKE\",\"speed\":32.4,\"spin\":2800},"
                        + "{\"type\":\"BACKHAND_GROUNDSTROKE\",\"speed\":29.1,\"spin\":2400},"
                        + "{\"type\":\"FOREHAND_GROUNDSTROKE\",\"speed\":31.0,\"spin\":2600}"
                        + "],\"rally\":"
                        + (4 + (sequence % 5))
                        + ",\"seed\":"
                        + seed
                        + "}";
        return jobId
                + "\t"
                + matchId
                + "\t"
                + sequence
                + "\t"
                + server
                + "\t"
                + winner
                + "\tWINNER\t"
                + (4 + (sequence % 5))
                + "\t"
                + score
                + "\t"
                + shots
                + "\n";
    }

    public static String productionTape(
            UUID jobId, UUID matchId, UUID home, UUID away, int rows, long seed) {
        StringBuilder out = new StringBuilder(rows * 420);
        for (int sequence = 1; sequence <= rows; sequence++) {
            out.append(productionRow(jobId, matchId, home, away, sequence, seed));
        }
        return out.toString();
    }
}
