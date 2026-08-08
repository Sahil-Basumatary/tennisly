package dev.sahilbasumatary.userservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sahilbasumatary.userservice.context.RequestContext;
import dev.sahilbasumatary.userservice.dto.request.AdminUpdateOrganizationRequest;
import dev.sahilbasumatary.userservice.entity.Organization;
import dev.sahilbasumatary.userservice.entity.PlanTier;
import dev.sahilbasumatary.userservice.exception.ResourceNotFoundException;
import dev.sahilbasumatary.userservice.exception.UnauthorizedAccessException;
import dev.sahilbasumatary.userservice.repository.OrganizationRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class AdminOrganizationServiceTest {

    private static final UUID ORG_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @Mock private OrganizationRepository organizationRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private UsageMeter usageMeter;
    @InjectMocks private AdminOrganizationService service;

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
    void listRequiresPlatformAdmin() {
        RequestContext.setRoles(Set.of("MEMBER"));
        assertThrows(UnauthorizedAccessException.class, () -> service.list(null, null, 0, 20));
    }

    @Test
    void listReturnsPagedOrganizations() {
        Organization org = sampleOrg(true);
        when(organizationRepository.search(isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(org)));
        var page = service.list(null, null, 0, 20);
        assertEquals(1, page.content().size());
        assertEquals(ORG_ID, page.content().get(0).id());
        verify(organizationRepository).search(isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void getFindsInactiveOrganization() {
        Organization org = sampleOrg(false);
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));
        var response = service.get(ORG_ID);
        assertEquals(false, response.active());
        assertEquals("org_clerk_1", response.clerkOrgId());
    }

    @Test
    void getThrowsWhenMissing() {
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.get(ORG_ID));
    }

    @Test
    void updateAppliesAdminFields() {
        Organization org = sampleOrg(true);
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));
        when(organizationRepository.save(org)).thenReturn(org);
        var request =
                new AdminUpdateOrganizationRequest(
                        "Renamed Club",
                        null,
                        null,
                        null,
                        PlanTier.PRO,
                        50,
                        false);
        var response = service.update(ORG_ID, request);
        assertEquals("Renamed Club", response.name());
        assertEquals(PlanTier.PRO, response.planTier());
        assertEquals(50, response.maxMembers());
        assertEquals(false, response.active());
        verify(organizationRepository).save(org);
    }

    private Organization sampleOrg(boolean active) {
        Organization org = new Organization();
        org.setId(ORG_ID);
        org.setClerkOrgId("org_clerk_1");
        org.setName("Baseline Club");
        org.setSlug("baseline-club");
        org.setPlanTier(PlanTier.FREE);
        org.setMaxMembers(10);
        org.setActive(active);
        org.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));
        org.setUpdatedAt(Instant.parse("2025-01-02T00:00:00Z"));
        return org;
    }
}
