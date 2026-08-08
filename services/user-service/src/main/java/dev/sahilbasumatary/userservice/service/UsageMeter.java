package dev.sahilbasumatary.userservice.service;

import dev.sahilbasumatary.userservice.dto.response.AdminUsageDailyResponse;
import dev.sahilbasumatary.userservice.dto.response.AdminUsageResponse;
import dev.sahilbasumatary.userservice.entity.Organization;
import dev.sahilbasumatary.userservice.entity.UsageDaily;
import dev.sahilbasumatary.userservice.exception.ResourceNotFoundException;
import dev.sahilbasumatary.userservice.repository.OrganizationRepository;
import dev.sahilbasumatary.userservice.repository.UsageDailyRepository;
import dev.sahilbasumatary.userservice.security.AdminAccess;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsageMeter {

    private final UsageDailyRepository usageDailyRepository;
    private final OrganizationRepository organizationRepository;

    public UsageMeter(
            UsageDailyRepository usageDailyRepository, OrganizationRepository organizationRepository) {
        this.usageDailyRepository = usageDailyRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public void increment(UUID organizationId, String metric, long delta) {
        if (delta <= 0) {
            return;
        }
        LocalDate day = LocalDate.now(ZoneOffset.UTC);
        UsageDaily row =
                usageDailyRepository
                        .findByOrganizationIdAndMetricAndDay(organizationId, metric, day)
                        .orElseGet(() -> {
                            Organization org =
                                    organizationRepository
                                            .findById(organizationId)
                                            .orElseThrow(
                                                    () ->
                                                            new ResourceNotFoundException(
                                                                    "Organization", organizationId));
                            UsageDaily created = new UsageDaily();
                            created.setOrganization(org);
                            created.setMetric(metric);
                            created.setDay(day);
                            created.setCount(0);
                            return created;
                        });
        row.setCount(row.getCount() + delta);
        usageDailyRepository.save(row);
    }

    @Transactional(readOnly = true)
    public AdminUsageResponse list(UUID organizationId, LocalDate from, LocalDate to) {
        AdminAccess.assertPlatformAdmin();
        organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId));
        List<AdminUsageDailyResponse> daily =
                usageDailyRepository.findByOrganizationAndRange(organizationId, from, to).stream()
                        .map(AdminUsageDailyResponse::from)
                        .toList();
        Map<String, Long> totals = new HashMap<>();
        for (AdminUsageDailyResponse row : daily) {
            totals.merge(row.metric(), row.count(), Long::sum);
        }
        return new AdminUsageResponse(daily, totals);
    }
}
