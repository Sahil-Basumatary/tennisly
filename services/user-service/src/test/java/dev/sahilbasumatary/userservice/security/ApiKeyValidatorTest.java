package dev.sahilbasumatary.userservice.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import dev.sahilbasumatary.userservice.entity.Organization;
import dev.sahilbasumatary.userservice.entity.OrganizationApiKey;
import dev.sahilbasumatary.userservice.repository.OrganizationApiKeyRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiKeyValidatorTest {

    private static final UUID ORG_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final String PLAINTEXT = "tly_live_test_secret_key_value_here";

    @Mock private OrganizationApiKeyRepository apiKeyRepository;
    @InjectMocks private ApiKeyValidator validator;

    @Test
    void validateAcceptsActiveKey() {
        OrganizationApiKey key = sampleKey(true, null, null);
        when(apiKeyRepository.findByKeyHash(ApiKeyHasher.hash(PLAINTEXT))).thenReturn(Optional.of(key));
        assertTrue(validator.validate(PLAINTEXT).isPresent());
    }

    @Test
    void validateRejectsBlankKey() {
        assertTrue(validator.validate("  ").isEmpty());
        assertTrue(validator.validate(null).isEmpty());
    }

    @Test
    void validateRejectsInactiveKey() {
        OrganizationApiKey key = sampleKey(false, null, null);
        when(apiKeyRepository.findByKeyHash(ApiKeyHasher.hash(PLAINTEXT))).thenReturn(Optional.of(key));
        assertTrue(validator.validate(PLAINTEXT).isEmpty());
    }

    @Test
    void validateRejectsRevokedKey() {
        OrganizationApiKey key = sampleKey(true, null, Instant.parse("2025-01-01T00:00:00Z"));
        when(apiKeyRepository.findByKeyHash(ApiKeyHasher.hash(PLAINTEXT))).thenReturn(Optional.of(key));
        assertTrue(validator.validate(PLAINTEXT).isEmpty());
    }

    @Test
    void validateRejectsExpiredKey() {
        OrganizationApiKey key = sampleKey(true, Instant.parse("2020-01-01T00:00:00Z"), null);
        when(apiKeyRepository.findByKeyHash(ApiKeyHasher.hash(PLAINTEXT))).thenReturn(Optional.of(key));
        assertTrue(validator.validate(PLAINTEXT).isEmpty());
    }

    private OrganizationApiKey sampleKey(boolean active, Instant expiresAt, Instant revokedAt) {
        Organization org = new Organization();
        org.setId(ORG_ID);
        OrganizationApiKey key = new OrganizationApiKey();
        key.setId(UUID.randomUUID());
        key.setOrganization(org);
        key.setName("Partner feed");
        key.setKeyPrefix("tly_live_ab12");
        key.setKeyHash(ApiKeyHasher.hash(PLAINTEXT));
        key.setScopes(List.of("read"));
        key.setActive(active);
        key.setExpiresAt(expiresAt);
        key.setRevokedAt(revokedAt);
        key.setCreatedByClerkId("admin_clerk");
        return key;
    }
}
