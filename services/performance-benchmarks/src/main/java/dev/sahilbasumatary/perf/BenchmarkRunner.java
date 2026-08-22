package dev.sahilbasumatary.perf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public final class BenchmarkRunner {

    private static final double P99_BUDGET_NS = 1_000_000d;
    private static final double POINT_DECISION_FLOOR_OPS_PER_SEC = 1_000_000d;

    private BenchmarkRunner() {}

    public static void main(String[] args) throws Exception {
        Path out = Path.of(System.getProperty("jmh.output", "target/jmh-result.json"));
        Path parent = out.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        boolean quick = Boolean.parseBoolean(System.getenv().getOrDefault("JMH_QUICK", "false"));
        int forks = Integer.parseInt(System.getenv().getOrDefault("JMH_FORKS", quick ? "1" : "2"));
        // ForkedMain lives on this process classpath only if we launched a shaded uber-jar.
        ChainedOptionsBuilder builder =
                new OptionsBuilder()
                        .include(
                                HotPathBenchmark.class.getSimpleName()
                                        + "|"
                                        + PointDecisionThroughputBenchmark.class.getSimpleName())
                        .result(out.toString())
                        .resultFormat(ResultFormatType.JSON)
                        .forks(forks)
                        .warmupIterations(quick ? 1 : 3)
                        .measurementIterations(quick ? 2 : 5)
                        .shouldFailOnError(true)
                        .jvmArgsAppend("-Xms256m", "-Xmx256m");
        if (!quick) {
            builder.addProfiler(GCProfiler.class);
        }
        Options options = builder.build();
        new Runner(options).run();
        gate(out);
    }

    static void gate(Path resultJson) throws Exception {
        JsonNode root = new ObjectMapper().readTree(resultJson.toFile());
        if (!root.isArray() || root.isEmpty()) {
            throw new IllegalStateException("JMH result has no benchmarks: " + resultJson);
        }
        List<String> failures = new ArrayList<>();
        boolean foundLatency = false;
        boolean foundPointDecisionThroughput = false;
        for (JsonNode bench : root) {
            String name = bench.path("benchmark").asText();
            String mode = bench.path("mode").asText();
            if ("thrpt".equals(mode)) {
                if (name.endsWith(".pointDecisionPipeline")) {
                    foundPointDecisionThroughput = true;
                    double score = bench.path("primaryMetric").path("score").asDouble();
                    String unit = bench.path("primaryMetric").path("scoreUnit").asText();
                    if (!"ops/s".equals(unit) || score < POINT_DECISION_FLOOR_OPS_PER_SEC) {
                        failures.add(name + " throughput=" + score + " " + unit);
                    }
                }
                continue;
            }
            foundLatency = true;
            JsonNode p99 = bench.path("primaryMetric").path("scorePercentiles").path("99.0");
            if (!p99.isNumber()) {
                failures.add(name + " missing p99");
                continue;
            }
            double value = p99.asDouble();
            String unit = bench.path("primaryMetric").path("scoreUnit").asText();
            double nanos = toNanos(value, unit);
            if (nanos > P99_BUDGET_NS) {
                failures.add(name + " p99=" + nanos + "ns unit=" + unit);
            }
        }
        if (!foundLatency) {
            failures.add("missing latency benchmarks");
        }
        if (!foundPointDecisionThroughput) {
            failures.add("missing point-decision throughput benchmark");
        }
        if (!failures.isEmpty()) {
            throw new IllegalStateException("JMH performance budget missed: " + failures);
        }
    }

    private static double toNanos(double value, String unit) {
        if (unit.contains("us") || unit.contains("μs") || unit.contains("µs")) {
            return value * 1_000d;
        }
        if (unit.contains("ms")) {
            return value * 1_000_000d;
        }
        if (unit.contains("s/op") && !unit.contains("ns") && !unit.contains("ms")) {
            return value * 1_000_000_000d;
        }
        return value;
    }
}
