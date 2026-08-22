package dev.sahilbasumatary.perf;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BenchmarkRunnerTest {

    @TempDir Path tmp;

    @Test
    void gateRejectsEmptyResult() throws Exception {
        Path empty = tmp.resolve("empty.json");
        Files.writeString(empty, "[]");
        assertNotNull(
                assertThrows(IllegalStateException.class, () -> BenchmarkRunner.gate(empty))
                        .getMessage());
    }

    @Test
    void gateAcceptsSubMillisecondP99() throws Exception {
        Path ok = tmp.resolve("ok.json");
        Files.writeString(
                ok,
                """
                [
                  {
                    "benchmark":"demo.latency",
                    "mode":"sample",
                    "primaryMetric":{"scorePercentiles":{"99.0":400.0},"scoreUnit":"ns/op"}
                  },
                  {
                    "benchmark":"demo.pointDecisionPipeline",
                    "mode":"thrpt",
                    "primaryMetric":{"score":1500000.0,"scoreUnit":"ops/s"}
                  }
                ]
                """);
        assertDoesNotThrow(() -> BenchmarkRunner.gate(ok));
    }

    @Test
    void gateRejectsLowPointDecisionThroughput() throws Exception {
        Path slow = tmp.resolve("slow.json");
        Files.writeString(
                slow,
                """
                [
                  {
                    "benchmark":"demo.latency",
                    "mode":"sample",
                    "primaryMetric":{"scorePercentiles":{"99.0":400.0},"scoreUnit":"ns/op"}
                  },
                  {
                    "benchmark":"demo.pointDecisionPipeline",
                    "mode":"thrpt",
                    "primaryMetric":{"score":999999.0,"scoreUnit":"ops/s"}
                  }
                ]
                """);
        assertNotNull(
                assertThrows(IllegalStateException.class, () -> BenchmarkRunner.gate(slow))
                        .getMessage());
    }
}
