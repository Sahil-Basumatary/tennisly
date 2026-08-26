package dev.sahilbasumatary.perf;

import dev.sahilbasumatary.analyticsservice.archive.ArchiveEventGenerator;
import dev.sahilbasumatary.analyticsservice.archive.ArchiveProcessResult;
import dev.sahilbasumatary.analyticsservice.archive.ArchiveTapeProcessor;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 1, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(
        value = 1,
        jvmArgsAppend = {"-Xms512m", "-Xmx512m"})
@State(Scope.Thread)
public class ArchiveThroughputBenchmark {

    @Param({"1", "8"})
    public int workers;

    private ArchiveTapeProcessor processor;
    private ArchiveEventGenerator.Dataset dataset;

    @Setup
    public void setup() {
        processor = new ArchiveTapeProcessor();
        dataset = ArchiveEventGenerator.generate(ArchiveEventGenerator.Spec.million());
    }

    @Benchmark
    public ArchiveProcessResult processMillionEvents(Blackhole blackhole) {
        return runTape(blackhole);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public ArchiveProcessResult processMillionEventsSample(Blackhole blackhole) {
        return runTape(blackhole);
    }

    private ArchiveProcessResult runTape(Blackhole blackhole) {
        ArchiveProcessResult result =
                processor.process(dataset.events(), dataset.rosters(), workers);
        blackhole.consume(result.fingerprint());
        blackhole.consume(result.accepted());
        return result;
    }
}
