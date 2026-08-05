package dev.sahilbasumatary.tennisdataservice.dto.response;

import dev.sahilbasumatary.tennisdataservice.dto.UpstreamPointData;
import java.util.Map;

public record UpstreamPointResponse(
        int sequenceNumber,
        int serverSide,
        int winnerSide,
        String outcome,
        Integer rallyLength,
        Map<String, Object> scoreSnapshot) {

    public static UpstreamPointResponse from(UpstreamPointData point) {
        return new UpstreamPointResponse(
                point.sequenceNumber(),
                point.serverSide(),
                point.winnerSide(),
                point.outcome(),
                point.rallyLength(),
                point.scoreSnapshot());
    }
}
