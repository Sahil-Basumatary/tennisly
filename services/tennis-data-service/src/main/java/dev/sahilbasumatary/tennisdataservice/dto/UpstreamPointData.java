package dev.sahilbasumatary.tennisdataservice.dto;

import java.util.Map;

public record UpstreamPointData(
        int sequenceNumber,
        int serverSide,
        int winnerSide,
        String outcome,
        Integer rallyLength,
        Map<String, Object> scoreSnapshot) {}
