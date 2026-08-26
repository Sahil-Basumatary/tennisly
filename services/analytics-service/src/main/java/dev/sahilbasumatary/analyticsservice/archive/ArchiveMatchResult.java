package dev.sahilbasumatary.analyticsservice.archive;

import dev.sahilbasumatary.analyticsservice.domain.TapeMatchMetrics;
import java.util.UUID;

public record ArchiveMatchResult(
        UUID matchId, int accepted, int duplicates, int gaps, TapeMatchMetrics metrics) {}
