package dev.sahilbasumatary.userservice.security;

import dev.sahilbasumatary.userservice.entity.OrganizationApiKey;
import dev.sahilbasumatary.userservice.repository.OrganizationApiKeyRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyValidator {

    private final OrganizationApiKeyRepository apiKeyRepository;

    public ApiKeyValidator(OrganizationApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    public Optional<OrganizationApiKey> validate(String plaintextKey) {
        if (plaintextKey == null || plaintextKey.isBlank()) {
            return Optional.empty();
        }
        String hash = ApiKeyHasher.hash(plaintextKey.trim());
        return apiKeyRepository
                .findByKeyHash(hash)
                .filter(OrganizationApiKey::isActive)
                .filter(key -> key.getRevokedAt() == null)
                .filter(key -> key.getExpiresAt() == null || key.getExpiresAt().isAfter(Instant.now()));
    }
}
