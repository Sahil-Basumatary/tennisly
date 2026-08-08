package dev.sahilbasumatary.analyticsservice.dto.response;

import java.time.Instant;
import java.util.List;

public record MatchReportResponse(
        Instant generatedAt,
        String title,
        List<MatchReportSectionResponse> sections,
        MatchAnalyticsResponse match) {}
