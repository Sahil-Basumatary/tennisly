package dev.sahilbasumatary.analyticsservice.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArchiveTapeProcessorTest {

    private final ArchiveTapeProcessor processor = new ArchiveTapeProcessor();

    @Test
    void compactTapeConservesCountsAndIsIndependentOfWorkers() {
        ArchiveEventGenerator.Dataset dataset =
                ArchiveEventGenerator.generate(ArchiveEventGenerator.Spec.compact());
        ArchiveProcessResult one = processor.process(dataset.events(), dataset.rosters(), 1);
        final ArchiveProcessResult four =
                processor.process(dataset.events(), dataset.rosters(), 4);
        assertEquals(dataset.events().size(), one.sourceRows());
        assertEquals(dataset.expectedAccepted(), one.accepted());
        assertEquals(dataset.expectedDuplicates(), one.duplicates());
        assertEquals(dataset.expectedGaps(), one.gapCount());
        assertEquals(one.sourceRows(), one.accepted() + one.duplicates());
        assertEquals(one.fingerprint(), four.fingerprint());
        assertEquals(one.accepted(), four.accepted());
        assertEquals(one.duplicates(), four.duplicates());
        assertEquals(one.gapCount(), four.gapCount());
        assertTrue(one.matchCount() > 0);
    }

    @Test
    void millionEventTapeStaysDeterministicAcrossThreadCounts() {
        ArchiveEventGenerator.Dataset dataset =
                ArchiveEventGenerator.generate(ArchiveEventGenerator.Spec.million());
        ArchiveProcessResult one = processor.process(dataset.events(), dataset.rosters(), 1);
        final ArchiveProcessResult ten =
                processor.process(dataset.events(), dataset.rosters(), 10);
        long started = System.nanoTime();
        final ArchiveProcessResult timed =
                processor.process(dataset.events(), dataset.rosters(), 8);
        double seconds = (System.nanoTime() - started) / 1_000_000_000.0;
        double eventsPerSecond = dataset.events().size() / seconds;
        assertTrue(
                eventsPerSecond >= 100_000.0,
                "archive events/s=" + eventsPerSecond + " must clear the 100k floor");
        assertEquals(1_000, one.matchCount());
        assertEquals(dataset.expectedAccepted(), one.accepted());
        assertEquals(dataset.expectedDuplicates(), one.duplicates());
        assertEquals(dataset.expectedGaps(), one.gapCount());
        assertEquals(one.sourceRows(), one.accepted() + one.duplicates());
        assertEquals(one.fingerprint(), ten.fingerprint());
        assertEquals(one.fingerprint(), timed.fingerprint());
    }
}
