package dev.sahilbasumatary.userservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sahilbasumatary.userservice.context.RequestContext;
import dev.sahilbasumatary.userservice.dto.request.AdminCreateApiKeyRequest;
import dev.sahilbasumatary.userservice.entity.Organization;
import dev.sahilbasumatary.userservice.entity.OrganizationApiKey;
import dev.sahilbasumatary.userservice.exception.UnauthorizedAccessException;
import dev.sahilbasumatary.userservice.repository.OrganizationApiKeyRepository;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    private static final UUID ORG_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID KEY_ID = UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff");

    @Mock private OrganizationApiKeyRepository apiKeyRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private UsageMeter usageMeter;
    @InjectMocks private ApiKeyService service;

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
    void createReturnsPlaintextOnce() {
        Organization org = sampleOrg();
        when(organizationRepository.findById(ORG_ID)).thenReturn(Optional.of(org));
        when(apiKeyRepository.save(any(OrganizationApiKey.class)))
                .thenAnswer(
                        invocation -> {
                            OrganizationApiKey key = invocation.getArgument(0);
                            key.setId(KEY_ID);
                            key.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));
                            key.setUpdatedAt(Instant.parse("2025-01-01T00:00:00Z"));
                            return key;
                        });
        var request = new AdminCreateApiKeyRequest(ORG_ID, "Partner feed", List.of("read"), null);
        var response = service.create(request);
        assertNotNull(response.plaintextKey());
        assertTruePlaintextFormat(response.plaintextKey());
        assertEquals("Partner feed", response.key().name());
        assertNull(response.key().revokedAt());
        verify(auditLogService)
                .record(eq("API_KEY_CREATE"), eq("API_KEY"), eq(KEY_ID.toString()), eq(ORG_ID), any());
        verify(usageMeter).increment(ORG_ID, "admin_actions", 1);
    }

    @Test
    void listNeverIncludesPlaintextOrHash() {
        OrganizationApiKey key = sampleKey(true);
        when(apiKeyRepository.search(eq(ORG_ID), eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(key)));
        var page = service.list(ORG_ID, true, 0, 20);
        assertEquals(1, page.content().size());
        assertEquals("tly_live_ab12", page.content().get(0).keyPrefix());
    }

    @Test
    void revokeDeactivatesKey() {
        OrganizationApiKey key = sampleKey(true);
        when(apiKeyRepository.findById(KEY_ID)).thenReturn(Optional.of(key));
        when(apiKeyRepository.save(key)).thenReturn(key);
        var response = service.revoke(KEY_ID);
        assertEquals(false, response.active());
        assertNotNull(response.revokedAt());
        verify(auditLogService)
                .record(eq("API_KEY_REVOKE"), eq("API_KEY"), eq(KEY_ID.toString()), eq(ORG_ID), any());
        verify(usageMeter).increment(ORG_ID, "admin_actions", 1);
    }

    @Test
    void revokeIsIdempotentForInactiveKey() {
        OrganizationApiKey key = sampleKey(false);
        key.setRevokedAt(Instant.parse("2025-01-02T00:00:00Z"));
        when(apiKeyRepository.findById(KEY_ID)).thenReturn(Optional.of(key));
        service.revoke(KEY_ID);
        verify(auditLogService, never()).record(any(), any(), any(), any(), any());
        verify(apiKeyRepository, never()).save(any());
    }

    private Organization sampleOrg() {
        Organization org = new Organization();
        org.setId(ORG_ID);
        org.setName("Baseline Club");
        return org;
    }

    private OrganizationApiKey sampleKey(boolean active) {
        OrganizationApiKey key = new OrganizationApiKey();
        key.setId(KEY_ID);
        key.setOrganization(sampleOrg());
        key.setName("Partner feed");
        key.setKeyPrefix("tly_live_ab12");
        key.setKeyHash("abc123");
        key.setScopes(List.of("read"));
        key.setActive(active);
        key.setCreatedByClerkId("admin_clerk");
        key.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));
        key.setUpdatedAt(Instant.parse("2025-01-01T00:00:00Z"));
        return key;
    }

    private void assertTruePlaintextFormat(String plaintext) {
        org.junit.jupiter.api.Assertions.assertTrue(plaintext.startsWith("tly_live_"));
    }
}
