package dev.sahilbasumatary.authservice.client;

import dev.sahilbasumatary.common.event.OrganizationEvent;
import dev.sahilbasumatary.common.event.UserEvent;
import dev.sahilbasumatary.common.security.InternalToken;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class UserProjectionClient {

    private static final Logger log = LoggerFactory.getLogger(UserProjectionClient.class);
    private final RestClient restClient;

    public UserProjectionClient(
            @Value("${tennisly.clients.user-service-uri:}") String userServiceUri,
            @Value("${tennisly.internal-token:}") String internalToken) {
        if (userServiceUri == null || userServiceUri.isBlank()) {
            this.restClient = null;
            return;
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(20));
        RestClient.Builder builder =
                RestClient.builder()
                        .baseUrl(userServiceUri.replaceAll("/$", ""))
                        .requestFactory(factory);
        if (InternalToken.isEnabled(internalToken)) {
            builder.defaultHeader(InternalToken.HEADER, internalToken);
        }
        this.restClient = builder.build();
    }

    public void relayUser(UserEvent event) {
        post("/internal/auth-events/users", event, event.getClerkId());
    }

    public void relayOrganization(OrganizationEvent event) {
        post("/internal/auth-events/organizations", event, event.getClerkOrgId());
    }

    private void post(String path, Object body, String key) {
        if (restClient == null) {
            return;
        }
        try {
            restClient
                    .post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.error("Failed to project auth event to user-service key={}: {}", key, ex.getMessage());
        }
    }
}
