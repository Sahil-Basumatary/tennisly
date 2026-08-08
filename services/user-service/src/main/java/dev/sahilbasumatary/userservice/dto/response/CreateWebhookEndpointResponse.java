package dev.sahilbasumatary.userservice.dto.response;

import dev.sahilbasumatary.userservice.entity.OrganizationWebhookEndpoint;

public record CreateWebhookEndpointResponse(
        WebhookEndpointResponse endpoint,
        String plaintextSecret) {

    public static CreateWebhookEndpointResponse from(
            OrganizationWebhookEndpoint entity, String plaintextSecret) {
        return new CreateWebhookEndpointResponse(
                WebhookEndpointResponse.from(entity), plaintextSecret);
    }
}
