package dev.sahilbasumatary.userservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sahilbasumatary.common.kafka.EventPublisher;
import dev.sahilbasumatary.userservice.context.RequestContext;
import dev.sahilbasumatary.userservice.dto.request.CreateWebhookEndpointRequest;
import dev.sahilbasumatary.userservice.entity.Organization;
import dev.sahilbasumatary.userservice.entity.OrganizationWebhookEndpoint;
import dev.sahilbasumatary.userservice.repository.OrganizationRepository;
import dev.sahilbasumatary.userservice.repository.OrganizationWebhookEndpointRepository;
import dev.sahilbasumatary.userservice.security.WebhookSecretCipher;
import dev.sahilbasumatary.userservice.security.WebhookUrlValidator;
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

@ExtendWith(MockitoExtension.class)
class WebhookEndpointServiceTest {

    private static final UUID ORG_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID ENDPOINT_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Mock private OrganizationWebhookEndpointRepository webhookRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private WebhookSecretCipher secretCipher;
    @Mock private WebhookUrlValidator urlValidator;
    @Mock private AuditLogService auditLogService;
    @Mock private UsageMeter usageMeter;
    @Mock private EventPublisher eventPublisher;
    @InjectMocks private WebhookEndpointService service;

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
    void createRejectsInvalidEventTypes() {
        var request = new CreateWebhookEndpointRequest(
                ORG_ID, "My Hook", "https://example.com/hook",
                List.of("invalid.event.type"), null);
        assertThrows(IllegalArgumentException.class,
                () -> service.create(request, ORG_ID));
    }

    @Test
    void createReturnsPlaintextSecretOnce() {
        Organization org = sampleOrg();
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));
        when(secretCipher.encrypt(any())).thenReturn("encrypted_blob");
        when(webhookRepository.save(any(OrganizationWebhookEndpoint.class)))
                .thenAnswer(invocation -> {
                    OrganizationWebhookEndpoint ep = invocation.getArgument(0);
                    ep.setId(ENDPOINT_ID);
                    ep.setCreatedAt(Instant.parse("2025-06-01T00:00:00Z"));
                    ep.setUpdatedAt(Instant.parse("2025-06-01T00:00:00Z"));
                    return ep;
                });
        var request = new CreateWebhookEndpointRequest(
                ORG_ID, "Match Events", "https://example.com/hook",
                List.of("match.completed"), "Match completion events");
        var response = service.create(request, ORG_ID);
        assertNotNull(response.plaintextSecret());
        assertEquals("Match Events", response.endpoint().name());
        verify(auditLogService).record(
                eq("WEBHOOK_CREATE"), eq("WEBHOOK_ENDPOINT"),
                eq(ENDPOINT_ID.toString()), eq(ORG_ID), any());
        verify(usageMeter).increment(ORG_ID, "admin_actions", 1);
    }

    @Test
    void createAcceptsMultipleValidEventTypes() {
        Organization org = sampleOrg();
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));
        when(secretCipher.encrypt(any())).thenReturn("encrypted_blob");
        when(webhookRepository.save(any(OrganizationWebhookEndpoint.class)))
                .thenAnswer(invocation -> {
                    OrganizationWebhookEndpoint ep = invocation.getArgument(0);
                    ep.setId(ENDPOINT_ID);
                    ep.setCreatedAt(Instant.parse("2025-06-01T00:00:00Z"));
                    ep.setUpdatedAt(Instant.parse("2025-06-01T00:00:00Z"));
                    return ep;
                });
        var request = new CreateWebhookEndpointRequest(
                ORG_ID, "All Events", "https://example.com/hook",
                List.of("match.completed", "api_key.revoked", "match.point_recorded"), null);
        var response = service.create(request, ORG_ID);
        assertEquals(3, response.endpoint().eventTypes().size());
    }

    private Organization sampleOrg() {
        Organization org = new Organization();
        org.setId(ORG_ID);
        org.setName("Baseline Club");
        return org;
    }
}
