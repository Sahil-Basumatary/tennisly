package dev.sahilbasumatary.analyticsservice.archive;

import java.util.List;

public record ArchiveProcessResult(
        int sourceRows,
        int accepted,
        int duplicates,
        int gapCount,
        int matchCount,
        String fingerprint,
        List<ArchiveMatchResult> matches) {}
