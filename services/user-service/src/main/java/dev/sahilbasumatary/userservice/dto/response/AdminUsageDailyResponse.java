package dev.sahilbasumatary.userservice.dto.response;

import dev.sahilbasumatary.userservice.entity.UsageDaily;
import java.time.LocalDate;
import java.util.UUID;

public record AdminUsageDailyResponse(UUID organizationId, String metric, LocalDate day, long count) {

    public static AdminUsageDailyResponse from(UsageDaily row) {
        return new AdminUsageDailyResponse(
                row.getOrganization().getId(), row.getMetric(), row.getDay(), row.getCount());
    }
}
