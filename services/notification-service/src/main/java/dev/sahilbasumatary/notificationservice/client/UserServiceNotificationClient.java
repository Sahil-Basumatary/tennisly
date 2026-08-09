package dev.sahilbasumatary.notificationservice.client;

import dev.sahilbasumatary.notificationservice.client.dto.EmailPreferenceResponse;
import dev.sahilbasumatary.notificationservice.client.dto.EmailRecipientResponse;
import dev.sahilbasumatary.notificationservice.client.dto.PushPreferenceResponse;
import dev.sahilbasumatary.notificationservice.client.dto.PushRecipientResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class UserServiceNotificationClient {

    private final RestClient restClient;

    public UserServiceNotificationClient(
            @Qualifier("userServiceRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    public EmailPreferenceResponse getEmailPreference(String clerkId, String category) {
        return restClient
                .get()
                .uri(
                        "/internal/users/by-clerk/{clerkId}/email-preference?category={category}",
                        clerkId,
                        category)
                .retrieve()
                .body(EmailPreferenceResponse.class);
    }

    public List<EmailRecipientResponse> listOrgEmailRecipients(
            UUID organizationId, String category) {
        return restClient
                .get()
                .uri(
                        "/internal/organizations/{organizationId}/email-recipients?category={category}",
                        organizationId,
                        category)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public PushPreferenceResponse getPushPreference(String clerkId, String category) {
        return restClient
                .get()
                .uri(
                        "/internal/users/by-clerk/{clerkId}/push-preference?category={category}",
                        clerkId,
                        category)
                .retrieve()
                .body(PushPreferenceResponse.class);
    }

    public List<PushRecipientResponse> listOrgPushRecipients(UUID organizationId, String category) {
        return restClient
                .get()
                .uri(
                        "/internal/organizations/{organizationId}/push-recipients?category={category}",
                        organizationId,
                        category)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}
