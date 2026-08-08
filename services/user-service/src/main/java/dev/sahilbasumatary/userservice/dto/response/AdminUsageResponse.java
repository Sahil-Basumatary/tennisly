package dev.sahilbasumatary.userservice.dto.response;

import java.util.List;
import java.util.Map;

public record AdminUsageResponse(
        List<AdminUsageDailyResponse> daily, Map<String, Long> totalsByMetric) {}
