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
        boolean evidence =
                Boolean.parseBoolean(System.getenv().getOrDefault("JMH_EVIDENCE", "false"));
        String defaultForks = evidence ? "3" : quick ? "1" : "2";
        int forks = Integer.parseInt(System.getenv().getOrDefault("JMH_FORKS", defaultForks));
        String include =
                System.getenv()
                        .getOrDefault(
                                "JMH_INCLUDE",
                                HotPathBenchmark.class.getSimpleName()
                                        + "|"
                                        + PointDecisionThroughputBenchmark.class.getSimpleName()
                                        + "|"
                                        + PointCommitCpuBenchmark.class.getSimpleName());
        String heap = System.getenv().getOrDefault("JMH_HEAP", evidence ? "512m" : "256m");
        int warmup = quick ? 1 : evidence ? 5 : 3;
        int measurement = quick ? 2 : evidence ? 10 : 5;
        ChainedOptionsBuilder builder =
                new OptionsBuilder()
                        .include(include)
                        .result(out.toString())
                        .resultFormat(ResultFormatType.JSON)
                        .forks(forks)
                        .warmupIterations(warmup)
                        .measurementIterations(measurement)
                        .shouldFailOnError(true)
                        .jvmArgsAppend("-Xms" + heap, "-Xmx" + heap);
        if (!quick) {
            builder.addProfiler(GCProfiler.class);
        }
        String jfr = System.getenv().getOrDefault("JMH_JFR", "");
        if (jfr.isBlank() && evidence) {
            Path jfrPath = out.resolveSibling(out.getFileName().toString().replace(".json", ".jfr"));
            jfr = jfrPath.toString();
        }
        if (!jfr.isBlank()) {
            Path jfrFile = Path.of(jfr);
            Path jfrParent = jfrFile.getParent();
            if (jfrParent != null) {
                Files.createDirectories(jfrParent);
            }
            builder.jvmArgsAppend(
                    "-XX:StartFlightRecording=dumponexit=true,filename="
                            + jfrFile.toAbsolutePath()
                            + ",settings=profile,maxsize=64m");
        }
        Options options = builder.build();
        new Runner(options).run();
        if (Boolean.parseBoolean(System.getenv().getOrDefault("JMH_SKIP_GATE", "false"))) {
            return;
        }
        if (include.contains("ReplayPhysics") || include.contains("ArchiveThroughput")) {
            gateScale(out, include);
            return;
        }
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

    static void gateScale(Path resultJson, String include) throws Exception {
        JsonNode root = new ObjectMapper().readTree(resultJson.toFile());
        if (!root.isArray() || root.isEmpty()) {
            throw new IllegalStateException("JMH result has no benchmarks: " + resultJson);
        }
        List<String> failures = new ArrayList<>();
        if (include.contains("ArchiveThroughput")) {
            boolean found = false;
            for (JsonNode bench : root) {
                if (bench.path("benchmark").asText().endsWith(".processMillionEvents")
                        && "thrpt".equals(bench.path("mode").asText())) {
                    found = true;
                    double score = bench.path("primaryMetric").path("score").asDouble();
                    if (score < 0.1d) {
                        failures.add(
                                bench.path("benchmark").asText()
                                        + " tape/s="
                                        + score
                                        + " (100k events/s floor)");
                    }
                }
            }
            if (!found) {
                failures.add("missing archive throughput benchmark");
            }
        }
        if (include.contains("ReplayPhysics")) {
            boolean found = false;
            for (JsonNode bench : root) {
                if (bench.path("benchmark").asText().endsWith(".fullPointPipeline")
                        && "thrpt".equals(bench.path("mode").asText())) {
                    found = true;
                    double score = bench.path("primaryMetric").path("score").asDouble();
                    if (score < 10d) {
                        failures.add(
                                bench.path("benchmark").asText()
                                        + " full-pipeline points/s="
                                        + score);
                    }
                }
            }
            if (!found) {
                failures.add("missing replay full-pipeline benchmark");
            }
        }
        if (!failures.isEmpty()) {
            throw new IllegalStateException("JMH scale budget missed: " + failures);
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
