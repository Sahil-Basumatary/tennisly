package dev.sahilbasumatary.userservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sahilbasumatary.userservice.context.RequestContext;
import dev.sahilbasumatary.userservice.entity.Organization;
import dev.sahilbasumatary.userservice.entity.UsageDaily;
import dev.sahilbasumatary.userservice.exception.UnauthorizedAccessException;
import dev.sahilbasumatary.userservice.repository.OrganizationRepository;
import dev.sahilbasumatary.userservice.repository.UsageDailyRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsageMeterTest {

    private static final UUID ORG_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Mock private UsageDailyRepository usageDailyRepository;
    @Mock private OrganizationRepository organizationRepository;
    @InjectMocks private UsageMeter usageMeter;

    @BeforeEach
    void setUp() {
        RequestContext.setUserId("admin_clerk");
        RequestContext.setRoles(Set.of("ADMIN"));
    }

    @AfterEach
    void tearDown() {
        RequestContext.clear();
    }

    @Test
    void incrementUpsertsDailyCount() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        UsageDaily existing = new UsageDaily();
        existing.setOrganization(sampleOrg());
        existing.setMetric("admin_actions");
        existing.setDay(today);
        existing.setCount(2);
        when(usageDailyRepository.findByOrganizationIdAndMetricAndDay(ORG_ID, "admin_actions", today))
                .thenReturn(Optional.of(existing));
        when(usageDailyRepository.save(existing)).thenReturn(existing);
        usageMeter.increment(ORG_ID, "admin_actions", 3);
        assertEquals(5, existing.getCount());
        verify(usageDailyRepository).save(existing);
    }

    @Test
    void incrementCreatesRowWhenMissing() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Organization org = sampleOrg();
        when(usageDailyRepository.findByOrganizationIdAndMetricAndDay(ORG_ID, "admin_actions", today))
                .thenReturn(Optional.empty());
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));
        when(usageDailyRepository.save(any(UsageDaily.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        usageMeter.increment(ORG_ID, "admin_actions", 1);
        ArgumentCaptor<UsageDaily> captor = ArgumentCaptor.forClass(UsageDaily.class);
        verify(usageDailyRepository).save(captor.capture());
        UsageDaily created = captor.getValue();
        assertEquals(1, created.getCount());
        assertEquals("admin_actions", created.getMetric());
        assertEquals(today, created.getDay());
    }

    @Test
    void listRequiresPlatformAdmin() {
        RequestContext.setRoles(Set.of("MEMBER"));
        assertThrows(
                UnauthorizedAccessException.class,
                () -> usageMeter.list(ORG_ID, null, null));
    }

    @Test
    void listAggregatesTotalsByMetric() {
        Organization org = sampleOrg();
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));
        UsageDaily row1 = new UsageDaily();
        row1.setOrganization(org);
        row1.setMetric("admin_actions");
        row1.setDay(LocalDate.parse("2025-01-01"));
        row1.setCount(2);
        UsageDaily row2 = new UsageDaily();
        row2.setOrganization(org);
        row2.setMetric("admin_actions");
        row2.setDay(LocalDate.parse("2025-01-02"));
        row2.setCount(3);
        UsageDaily row3 = new UsageDaily();
        row3.setOrganization(org);
        row3.setMetric("api_requests");
        row3.setDay(LocalDate.parse("2025-01-02"));
        row3.setCount(10);
        when(usageDailyRepository.findByOrganizationAndRange(eq(ORG_ID), any(), any()))
                .thenReturn(List.of(row1, row2, row3));
        var response = usageMeter.list(ORG_ID, null, null);
        assertEquals(3, response.daily().size());
        assertEquals(5L, response.totalsByMetric().get("admin_actions"));
        assertEquals(10L, response.totalsByMetric().get("api_requests"));
    }

    private Organization sampleOrg() {
        Organization org = new Organization();
        org.setId(ORG_ID);
        org.setName("Baseline Club");
        return org;
    }
}
