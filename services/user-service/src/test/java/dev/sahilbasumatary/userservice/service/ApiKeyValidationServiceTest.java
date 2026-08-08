package dev.sahilbasumatary.userservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sahilbasumatary.userservice.dto.response.ApiKeyValidationResponse;
import dev.sahilbasumatary.userservice.entity.Organization;
import dev.sahilbasumatary.userservice.entity.OrganizationApiKey;
import dev.sahilbasumatary.userservice.entity.PlanTier;
import dev.sahilbasumatary.userservice.repository.OrganizationApiKeyRepository;
import dev.sahilbasumatary.userservice.security.ApiKeyValidator;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiKeyValidationServiceTest {

    private static final UUID ORG_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID KEY_ID = UUID.fromString("bbbbbbbb-cccc-dddd-eeee-ffffffffffff");

    @Mock private ApiKeyValidator apiKeyValidator;
    @Mock private OrganizationApiKeyRepository apiKeyRepository;
    @Mock private UsageMeter usageMeter;
    @InjectMocks private ApiKeyValidationService service;

    @Test
    void validateSuccessUpdatesLastUsedAndIncrementsUsage() {
        OrganizationApiKey key = sampleKey(true, null, null);
        when(apiKeyValidator.validate("tly_live_secret")).thenReturn(Optional.of(key));
        when(apiKeyRepository.save(key)).thenReturn(key);
        Optional<ApiKeyValidationResponse> response = service.validate("tly_live_secret");
        assertTrue(response.isPresent());
        assertEquals(ORG_ID, response.get().organizationId());
        assertEquals(KEY_ID, response.get().apiKeyId());
        assertEquals(List.of("read"), response.get().scopes());
        assertEquals("PRO", response.get().planTier());
        assertEquals("Baseline Club", response.get().organizationName());
        ArgumentCaptor<OrganizationApiKey> captor = ArgumentCaptor.forClass(OrganizationApiKey.class);
        verify(apiKeyRepository).save(captor.capture());
        assertTrue(captor.getValue().getLastUsedAt().isBefore(Instant.now().plusSeconds(1)));
        verify(usageMeter).increment(ORG_ID, "api_requests", 1);
    }

    @Test
    void validateReturnsEmptyForUnknownKey() {
        when(apiKeyValidator.validate("tly_live_bad")).thenReturn(Optional.empty());
        assertTrue(service.validate("tly_live_bad").isEmpty());
        verify(apiKeyRepository, never()).save(any());
        verify(usageMeter, never()).increment(any(), any(), eq(1L));
    }

    @Test
    void validateReturnsEmptyForExpiredKey() {
        when(apiKeyValidator.validate("tly_live_expired")).thenReturn(Optional.empty());
        assertTrue(service.validate("tly_live_expired").isEmpty());
        verify(apiKeyRepository, never()).save(any());
    }

    @Test
    void validateReturnsEmptyForRevokedKey() {
        when(apiKeyValidator.validate("tly_live_revoked")).thenReturn(Optional.empty());
        assertTrue(service.validate("tly_live_revoked").isEmpty());
        verify(apiKeyRepository, never()).save(any());
    }

    private OrganizationApiKey sampleKey(boolean active, Instant expiresAt, Instant revokedAt) {
        Organization org = new Organization();
        org.setId(ORG_ID);
        org.setName("Baseline Club");
        org.setPlanTier(PlanTier.PRO);
        OrganizationApiKey key = new OrganizationApiKey();
        key.setId(KEY_ID);
        key.setOrganization(org);
        key.setName("Partner feed");
        key.setKeyPrefix("tly_live_ab12");
        key.setKeyHash("abc123");
        key.setScopes(List.of("read"));
        key.setActive(active);
        key.setExpiresAt(expiresAt);
        key.setRevokedAt(revokedAt);
        key.setCreatedByClerkId("admin_clerk");
        return key;
    }
}
