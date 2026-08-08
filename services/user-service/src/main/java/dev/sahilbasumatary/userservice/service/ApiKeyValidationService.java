package dev.sahilbasumatary.userservice.service;

import dev.sahilbasumatary.userservice.dto.response.ApiKeyValidationResponse;
import dev.sahilbasumatary.userservice.entity.Organization;
import dev.sahilbasumatary.userservice.entity.OrganizationApiKey;
import dev.sahilbasumatary.userservice.repository.OrganizationApiKeyRepository;
import dev.sahilbasumatary.userservice.security.ApiKeyValidator;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApiKeyValidationService {

    private final ApiKeyValidator apiKeyValidator;
    private final OrganizationApiKeyRepository apiKeyRepository;
    private final UsageMeter usageMeter;

    public ApiKeyValidationService(
            ApiKeyValidator apiKeyValidator,
            OrganizationApiKeyRepository apiKeyRepository,
            UsageMeter usageMeter) {
        this.apiKeyValidator = apiKeyValidator;
        this.apiKeyRepository = apiKeyRepository;
        this.usageMeter = usageMeter;
    }

    @Transactional
    public Optional<ApiKeyValidationResponse> validate(String apiKey) {
        Optional<OrganizationApiKey> keyOpt = apiKeyValidator.validate(apiKey);
        if (keyOpt.isEmpty()) {
            return Optional.empty();
        }
        OrganizationApiKey key = keyOpt.get();
        key.setLastUsedAt(Instant.now());
        apiKeyRepository.save(key);
        Organization org = key.getOrganization();
        usageMeter.increment(org.getId(), "api_requests", 1);
        return Optional.of(
                new ApiKeyValidationResponse(
                        org.getId(),
                        key.getId(),
                        key.getScopes(),
                        org.getPlanTier().name(),
                        org.getName()));
    }
}
