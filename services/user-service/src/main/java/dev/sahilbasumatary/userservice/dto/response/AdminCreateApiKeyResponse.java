package dev.sahilbasumatary.userservice.dto.response;

import dev.sahilbasumatary.userservice.entity.OrganizationApiKey;

public record AdminCreateApiKeyResponse(AdminApiKeyResponse key, String plaintextKey) {

    public static AdminCreateApiKeyResponse from(OrganizationApiKey entity, String plaintextKey) {
        return new AdminCreateApiKeyResponse(AdminApiKeyResponse.from(entity), plaintextKey);
    }
}
