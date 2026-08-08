package dev.sahilbasumatary.analyticsservice.dto.response;

import dev.sahilbasumatary.analyticsservice.domain.TapeSideMetrics;
import java.util.List;
import java.util.UUID;

public record CompareResponse(
        int meetingCount,
        int aWins,
        int bWins,
        int unknownResults,
        List<HeadToHeadMeetingResponse> meetings,
        TapeSideMetrics playerA,
        TapeSideMetrics playerB) {}
