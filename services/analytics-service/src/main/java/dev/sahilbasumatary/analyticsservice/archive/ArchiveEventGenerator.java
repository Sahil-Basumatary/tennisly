package dev.sahilbasumatary.analyticsservice.archive;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.UUID;

/**
 * Builds a deterministic archive tape. Gaps are omitted sequences, not invented points; duplicates
 * and local shuffles are injected with a fixed seed so expected counts are known before processing.
 */
public final class ArchiveEventGenerator {

    private static final Map<String, Object> OPEN_GAME = Map.of();
    private static final Map<String, Object> BREAK_SNAPSHOT = Map.of("points", List.of("0", "0"));

    private ArchiveEventGenerator() {}

    public record Spec(
            long seed,
            int matchCount,
            int uniqueEvents,
            int duplicateCount,
            int gapCount,
            int shuffleWindow) {

        public static Spec million() {
            return new Spec(0xA11CE5EEDL, 1_000, 1_000_000, 2_000, 1_000, 16);
        }

        public static Spec compact() {
            return new Spec(0xC0FFEE54L, 10, 200, 8, 10, 8);
        }
    }

    public record Dataset(
            List<ArchiveEvent> events,
            List<ArchiveMatchRoster> rosters,
            int expectedAccepted,
            int expectedDuplicates,
            int expectedGaps) {}

    public static Dataset generate(Spec spec) {
        if (spec.matchCount() <= 0 || spec.uniqueEvents() < spec.matchCount()) {
            throw new IllegalArgumentException(
                    "archive spec needs at least one unique event per match");
        }
        if (spec.gapCount() < 0
                || spec.duplicateCount() < 0
                || spec.gapCount() >= spec.uniqueEvents()) {
            throw new IllegalArgumentException("archive spec gap/duplicate counts are invalid");
        }
        SplittableRandom random = new SplittableRandom(spec.seed());
        int pointsPerMatch = spec.uniqueEvents() / spec.matchCount();
        List<ArchiveMatchRoster> rosters = new ArrayList<>(spec.matchCount());
        List<ArchiveEvent> events = new ArrayList<>(spec.uniqueEvents() + spec.duplicateCount());
        int gapsInjected = 0;
        int duplicatesInjected = 0;
        for (int matchIndex = 0; matchIndex < spec.matchCount(); matchIndex++) {
            UUID matchId = uuid(spec.seed(), matchIndex, 1);
            UUID homeId = uuid(spec.seed(), matchIndex, 2);
            UUID awayId = uuid(spec.seed(), matchIndex, 3);
            rosters.add(new ArchiveMatchRoster(matchId, homeId, awayId));
            int remainingMatches = spec.matchCount() - matchIndex;
            int remainingGaps = spec.gapCount() - gapsInjected;
            int gapSequence = 0;
            if (remainingGaps > 0 && remainingGaps >= remainingMatches) {
                gapSequence = 1 + random.nextInt(Math.max(1, pointsPerMatch - 1));
            }
            for (int sequence = 1; sequence <= pointsPerMatch; sequence++) {
                if (sequence == gapSequence) {
                    gapsInjected += 1;
                    continue;
                }
                ArchiveEvent event = point(matchId, homeId, awayId, sequence, random);
                events.add(event);
                if (duplicatesInjected < spec.duplicateCount()
                        && random.nextInt(pointsPerMatch) == 0) {
                    events.add(event);
                    duplicatesInjected += 1;
                }
            }
        }
        while (duplicatesInjected < spec.duplicateCount() && !events.isEmpty()) {
            ArchiveEvent original = events.get(random.nextInt(events.size()));
            events.add(original);
            duplicatesInjected += 1;
        }
        shuffleWindows(events, spec.shuffleWindow(), random);
        int expectedAccepted = spec.uniqueEvents() - gapsInjected;
        return new Dataset(
                List.copyOf(events),
                List.copyOf(rosters),
                expectedAccepted,
                duplicatesInjected,
                gapsInjected);
    }

    private static ArchiveEvent point(
            UUID matchId, UUID homeId, UUID awayId, int sequence, SplittableRandom random) {
        boolean homeServes = sequence % 2 == 1;
        UUID serverId = homeServes ? homeId : awayId;
        boolean awayWon = random.nextBoolean();
        UUID winnerId = awayWon ? awayId : homeId;
        boolean breakPoint = !winnerId.equals(serverId) && random.nextInt(8) == 0;
        return new ArchiveEvent(
                matchId,
                sequence,
                sequence,
                serverId,
                winnerId,
                "WINNER",
                4,
                breakPoint ? BREAK_SNAPSHOT : OPEN_GAME);
    }

    private static void shuffleWindows(
            List<ArchiveEvent> events, int window, SplittableRandom random) {
        if (window <= 1 || events.size() < 2) {
            return;
        }
        for (int start = 0; start < events.size(); start += window) {
            int end = Math.min(events.size(), start + window);
            for (int index = end - 1; index > start; index--) {
                int swapWith = start + random.nextInt(index - start + 1);
                ArchiveEvent tmp = events.get(index);
                events.set(index, events.get(swapWith));
                events.set(swapWith, tmp);
            }
        }
    }

    private static UUID uuid(long seed, int a, int b) {
        long msb = seed ^ ((long) a * 0x9E3779B97F4A7C15L);
        long lsb = seed ^ ((long) b * 0xC13FA9A902A6328FL) ^ ((long) a << 32);
        return new UUID(msb, lsb);
    }
}
