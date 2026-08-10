package dev.sahilbasumatary.userservice.contract;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.sahilbasumatary.userservice.context.RequestContext;
import dev.sahilbasumatary.userservice.controller.PublicWebhookController;
import dev.sahilbasumatary.userservice.dto.response.WebhookEndpointResponse;
import dev.sahilbasumatary.userservice.service.WebhookEndpointService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Provider("user-service")
@PactFolder("pacts")
class PublicWebhookControllerProviderPactTest {

    private static final UUID ORG_ID =
            UUID.fromString("55555555-5555-5555-5555-555555555555");

    private final WebhookEndpointService webhookService = mock(WebhookEndpointService.class);

    @BeforeEach
    void setTarget(PactVerificationContext context) {
        if (context == null) {
            return;
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MappingJackson2HttpMessageConverter json = new MappingJackson2HttpMessageConverter(mapper);
        MockMvcTestTarget target = new MockMvcTestTarget();
        target.setControllers(new PublicWebhookController(webhookService));
        target.setMessageConverters(json);
        context.setTarget(target);
    }

    @AfterEach
    void clearTenant() {
        RequestContext.clear();
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void verifyPact(PactVerificationContext context) {
        context.verifyInteraction();
    }

    @State("webhook endpoints exist for org")
    void webhookEndpointsExistForOrg() {
        RequestContext.setOrgId(ORG_ID.toString());
        Instant stamped = Instant.parse("2024-01-01T12:00:00Z");
        when(webhookService.list(eq(ORG_ID)))
                .thenReturn(
                        List.of(
                                new WebhookEndpointResponse(
                                        UUID.fromString("66666666-6666-6666-6666-666666666666"),
                                        ORG_ID,
                                        "match-hooks",
                                        "https://hooks.example.com/tennisly",
                                        "whsec_abc12345",
                                        List.of("match.completed"),
                                        true,
                                        "prod deliveries",
                                        "user_abc",
                                        Instant.parse("2024-01-02T00:00:00Z"),
                                        Instant.parse("2024-01-03T00:00:00Z"),
                                        stamped,
                                        stamped)));
    }
}
