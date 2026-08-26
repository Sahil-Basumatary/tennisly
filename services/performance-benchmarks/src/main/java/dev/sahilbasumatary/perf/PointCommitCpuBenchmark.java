package dev.sahilbasumatary.perf;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sahilbasumatary.matchservice.entity.MatchStatus;
import dev.sahilbasumatary.matchservice.service.MatchStateMachine;
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
@Fork(
        value = 2,
        jvmArgsAppend = {"-Xms256m", "-Xmx256m"})
@State(Scope.Thread)
public class PointCommitCpuBenchmark {

    private static final String POINT_JSON =
            """
            {"serverId":"11111111-1111-1111-1111-111111111111",\
            "winnerId":"22222222-2222-2222-2222-222222222222",\
            "outcome":"WINNER","rallyLength":4,\
            "scoreSnapshot":{"set":1,"game":4,"points":["30","15"]},\
            "shotSummary":{"shots":3,"last":"FOREHAND_GROUNDSTROKE"}}
            """;

    private ObjectMapper objectMapper;
    private MatchStateMachine stateMachine;
    private UUID matchId;
    private long sequence;

    @Setup
    public void setup() {
        objectMapper = new ObjectMapper();
        stateMachine = new MatchStateMachine();
        matchId = UUID.fromString("33333333-3333-3333-3333-333333333333");
    }

    @Benchmark
    public String decodeValidateTransitionSerialize(Blackhole blackhole) throws Exception {
        PointCommitPayload point = objectMapper.readValue(POINT_JSON, PointCommitPayload.class);
        if (point.serverId() == null || point.winnerId() == null || point.outcome() == null) {
            throw new IllegalArgumentException("point payload missing required fields");
        }
        if (point.rallyLength() != null && point.rallyLength() < 0) {
            throw new IllegalArgumentException("rallyLength must be >= 0");
        }
        stateMachine.assertCanRecordPoint(MatchStatus.IN_PROGRESS);
        long next = ++sequence;
        String encoded =
                objectMapper.writeValueAsString(
                        Map.of(
                                "eventType",
                                "MATCH_POINT_RECORDED",
                                "matchId",
                                matchId,
                                "sequence",
                                next,
                                "winnerId",
                                point.winnerId(),
                                "outcome",
                                point.outcome(),
                                "scoreSnapshot",
                                point.scoreSnapshot()));
        blackhole.consume(point);
        return encoded;
    }

    public record PointCommitPayload(
            UUID serverId,
            UUID winnerId,
            String outcome,
            Integer rallyLength,
            Map<String, Object> scoreSnapshot,
            Map<String, Object> shotSummary) {}
}
