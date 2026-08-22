package dev.sahilbasumatary.perf;

import dev.sahilbasumatary.analyticsservice.client.dto.MatchPlayerSummary;
import dev.sahilbasumatary.analyticsservice.client.dto.MatchPointSummary;
import dev.sahilbasumatary.analyticsservice.client.dto.MatchSummary;
import dev.sahilbasumatary.analyticsservice.service.TapeMetricAggregator;
import dev.sahilbasumatary.apigateway.ratelimit.RateLimitDecision;
import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import dev.sahilbasumatary.matchservice.service.MatchStateMachine;
import dev.sahilbasumatary.userservice.security.ApiKeyHasher;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = {"-Xms256m", "-Xmx256m"})
@State(Scope.Benchmark)
public class HotPathBenchmark {

    private MatchStateMachine stateMachine;
    private TapeMetricAggregator aggregator;
    private MatchSummary match;
    private List<MatchPointSummary> points;
    private Instant now;
    private String apiKey;

    @Setup
    public void setup() {
        stateMachine = new MatchStateMachine();
        aggregator = new TapeMetricAggregator();
        now = Instant.parse("2026-01-15T12:00:00Z");
        apiKey = "tly_live_benchmark_fixture_key_value";
        UUID home = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID away = UUID.fromString("22222222-2222-2222-2222-222222222222");
        match =
                new MatchSummary(
                        UUID.fromString("33333333-3333-3333-3333-333333333333"),
                        "bench-1",
                        null,
                        "HARD",
                        "IN_PROGRESS",
                        3,
                        now,
                        now,
                        null,
                        Map.of(),
                        Map.of(),
                        List.of(
                                new MatchPlayerSummary(home, "Home", "HOME"),
                                new MatchPlayerSummary(away, "Away", "AWAY")),
                        8);
        points =
                List.of(
                        point(home, home),
                        point(home, away),
                        point(away, away),
                        point(home, home),
                        point(home, home),
                        point(away, home),
                        point(home, away),
                        point(away, away));
    }

    private static MatchPointSummary point(UUID server, UUID winner) {
        return new MatchPointSummary(
                UUID.randomUUID(), 1, server, winner, "WINNER", 4, Map.of("points", List.of("0", "0")));
    }

    @Benchmark
    public void rateLimitDecision(Blackhole blackhole) {
        blackhole.consume(RateLimitDecision.fromCount(12, 30, now));
    }

    @Benchmark
    public void matchStateValidation(Blackhole blackhole) {
        stateMachine.assertCanRecordPoint(MatchStatus.IN_PROGRESS);
        stateMachine.assertCanTransition(MatchStatus.IN_PROGRESS, MatchStatus.COMPLETED);
        blackhole.consume(MatchStatus.COMPLETED);
    }

    @Benchmark
    public void apiKeyHash(Blackhole blackhole) {
        blackhole.consume(ApiKeyHasher.hash(apiKey));
    }

    @Benchmark
    public void tapeAggregator(Blackhole blackhole) {
        blackhole.consume(aggregator.aggregate(match, points));
    }
}
