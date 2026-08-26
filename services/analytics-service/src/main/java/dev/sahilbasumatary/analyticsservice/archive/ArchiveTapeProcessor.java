package dev.sahilbasumatary.analyticsservice.archive;

import dev.sahilbasumatary.analyticsservice.client.dto.MatchPlayerSummary;
import dev.sahilbasumatary.analyticsservice.client.dto.MatchPointSummary;
import dev.sahilbasumatary.analyticsservice.client.dto.MatchSummary;
import dev.sahilbasumatary.analyticsservice.service.TapeMetricAggregator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Partitions a mixed archive tape by match, restores per-match order, rejects duplicate point
 * sequences, records gaps without inventing missing events, then aggregates accepted contiguous
 * data. Output is independent of worker count.
 */
public final class ArchiveTapeProcessor {

    public static final int STRIPE_COUNT = 1_024;

    private static final Instant EPOCH = Instant.parse("2026-01-01T00:00:00Z");
    private static final Comparator<ArchiveEvent> EVENT_ORDER =
            Comparator.comparingInt(ArchiveEvent::pointSequence)
                    .thenComparingLong(ArchiveEvent::liveSequence);

    private final TapeMetricAggregator aggregator;

    public ArchiveTapeProcessor() {
        this(new TapeMetricAggregator());
    }

    public ArchiveTapeProcessor(TapeMetricAggregator aggregator) {
        this.aggregator = aggregator;
    }

    public ArchiveProcessResult process(
            List<ArchiveEvent> events, List<ArchiveMatchRoster> rosters, int workers) {
        Map<UUID, ArchiveMatchRoster> rosterByMatch = new HashMap<>(rosters.size() * 2);
        for (ArchiveMatchRoster roster : rosters) {
            rosterByMatch.put(roster.matchId(), roster);
        }
        int poolSize = Math.max(1, workers);
        List<ArchiveMatchResult> results;
        if (poolSize == 1 || events.size() < 1_024) {
            results = processSerial(events, rosterByMatch);
        } else {
            results = processStriped(events, rosterByMatch, poolSize);
        }
        results.sort(Comparator.comparing(result -> result.matchId().toString()));
        int accepted = 0;
        int duplicates = 0;
        int gaps = 0;
        for (ArchiveMatchResult result : results) {
            accepted += result.accepted();
            duplicates += result.duplicates();
            gaps += result.gaps();
        }
        return new ArchiveProcessResult(
                events.size(),
                accepted,
                duplicates,
                gaps,
                results.size(),
                ArchiveFingerprint.sha256(results),
                List.copyOf(results));
    }

    private List<ArchiveMatchResult> processSerial(
            List<ArchiveEvent> events, Map<UUID, ArchiveMatchRoster> rosterByMatch) {
        Map<UUID, List<ArchiveEvent>> byMatch = HashMap.newHashMap(256);
        int bucketHint = Math.max(16, events.size() / Math.max(1, rosterByMatch.size()));
        for (ArchiveEvent event : events) {
            List<ArchiveEvent> bucket = byMatch.get(event.matchId());
            if (bucket == null) {
                bucket = new ArrayList<>(bucketHint);
                byMatch.put(event.matchId(), bucket);
            }
            bucket.add(event);
        }
        List<ArchiveMatchResult> results = new ArrayList<>(byMatch.size());
        for (Map.Entry<UUID, List<ArchiveEvent>> entry : byMatch.entrySet()) {
            results.add(processMatch(entry.getValue(), rosterByMatch.get(entry.getKey())));
        }
        return results;
    }

    private List<ArchiveMatchResult> processStriped(
            List<ArchiveEvent> events, Map<UUID, ArchiveMatchRoster> rosterByMatch, int workers) {
        @SuppressWarnings("unchecked")
        List<ArchiveEvent>[] stripes = new List[STRIPE_COUNT];
        int stripeCapacity = Math.max(32, (events.size() / STRIPE_COUNT) + 16);
        for (int stripe = 0; stripe < STRIPE_COUNT; stripe++) {
            stripes[stripe] = new ArrayList<>(stripeCapacity);
        }
        for (ArchiveEvent event : events) {
            int stripe = Math.floorMod(event.matchId().hashCode(), STRIPE_COUNT);
            stripes[stripe].add(event);
        }
        List<ArchiveMatchResult> results = new ArrayList<>(rosterByMatch.size());
        int poolSize = Math.min(workers, STRIPE_COUNT);
        try (ExecutorService pool = Executors.newFixedThreadPool(poolSize)) {
            List<Callable<List<ArchiveMatchResult>>> tasks = new ArrayList<>(poolSize);
            for (int worker = 0; worker < poolSize; worker++) {
                final int startStripe = worker;
                tasks.add(
                        () -> {
                            List<ArchiveMatchResult> local = new ArrayList<>();
                            for (int stripe = startStripe;
                                    stripe < STRIPE_COUNT;
                                    stripe += poolSize) {
                                List<ArchiveEvent> bucket = stripes[stripe];
                                if (!bucket.isEmpty()) {
                                    local.addAll(processSerial(bucket, rosterByMatch));
                                }
                            }
                            return local;
                        });
            }
            for (Future<List<ArchiveMatchResult>> future : pool.invokeAll(tasks)) {
                results.addAll(future.get());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while processing archive tape", ex);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("Failed to process archive tape", cause);
        }
        return results;
    }

    private ArchiveMatchResult processMatch(
            List<ArchiveEvent> unordered, ArchiveMatchRoster roster) {
        if (roster == null) {
            throw new IllegalArgumentException("missing roster for archive match");
        }
        unordered.sort(EVENT_ORDER);
        List<MatchPointSummary> accepted = new ArrayList<>(unordered.size());
        int duplicates = 0;
        int lastSequence = 0;
        int gaps = 0;
        for (ArchiveEvent event : unordered) {
            if (event.pointSequence() == lastSequence) {
                duplicates += 1;
                continue;
            }
            if (event.pointSequence() < lastSequence) {
                throw new IllegalStateException("archive sort failed for " + roster.matchId());
            }
            if (lastSequence > 0) {
                gaps += event.pointSequence() - lastSequence - 1;
            } else if (event.pointSequence() > 1) {
                gaps += event.pointSequence() - 1;
            }
            lastSequence = event.pointSequence();
            accepted.add(
                    new MatchPointSummary(
                            null,
                            event.pointSequence(),
                            event.serverId(),
                            event.winnerId(),
                            event.outcome(),
                            event.rallyLength(),
                            event.scoreSnapshot()));
        }
        MatchSummary summary = summaryFor(roster, accepted.size());
        return new ArchiveMatchResult(
                roster.matchId(),
                accepted.size(),
                duplicates,
                gaps,
                aggregator.aggregate(summary, accepted));
    }

    private static MatchSummary summaryFor(ArchiveMatchRoster roster, int pointsPlayed) {
        return new MatchSummary(
                roster.matchId(),
                roster.matchId().toString(),
                null,
                "HARD",
                "COMPLETED",
                3,
                EPOCH,
                EPOCH,
                EPOCH,
                Map.of(),
                Map.of(),
                List.of(
                        new MatchPlayerSummary(roster.homeId(), "Home", "HOME"),
                        new MatchPlayerSummary(roster.awayId(), "Away", "AWAY")),
                pointsPlayed);
    }
}
