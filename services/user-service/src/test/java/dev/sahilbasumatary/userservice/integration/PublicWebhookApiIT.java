package dev.sahilbasumatary.userservice.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sahilbasumatary.common.event.WebhookEventTypes;
import dev.sahilbasumatary.userservice.dto.request.CreateWebhookEndpointRequest;
import dev.sahilbasumatary.userservice.entity.Organization;
import dev.sahilbasumatary.userservice.entity.PlanTier;
import dev.sahilbasumatary.userservice.repository.OrganizationRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
@Testcontainers(disabledWithoutDocker = true)
class PublicWebhookApiIT {

    private static final String USER_ID = "user_it_clerk_1";

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("tennisly_users_it")
                    .withUsername("tennisly")
                    .withPassword("tennisly_dev");

    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    private UUID orgId;

    @BeforeEach
    void seedOrganization() {
        organizationRepository.deleteAll();
        Organization org = new Organization();
        org.setClerkOrgId("org_it_" + UUID.randomUUID());
        org.setName("IT Club");
        org.setSlug("it-club-" + UUID.randomUUID());
        org.setPlanTier(PlanTier.PRO);
        org.setMaxMembers(25);
        org.setActive(true);
        orgId = organizationRepository.save(org).getId();
    }

    @Test
    void createRejectsLoopbackTargetWhenPrivateTargetsDisallowed() throws Exception {
        var body =
                new CreateWebhookEndpointRequest(
                        null,
                        "ssrf-probe",
                        "https://127.0.0.1/hooks/tennisly",
                        List.of(WebhookEventTypes.MATCH_COMPLETED),
                        "should fail");
        mockMvc.perform(
                        post("/api/users/public/webhooks")
                                .header("X-User-Id", USER_ID)
                                .header("X-Org-Id", orgId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("loopback")));
    }

    @Test
    void createAndListWebhookForOrganization() throws Exception {
        var body =
                new CreateWebhookEndpointRequest(
                        null,
                        "match-hooks",
                        "https://example.com/hooks/tennisly",
                        List.of(WebhookEventTypes.MATCH_COMPLETED),
                        "prod receiver");
        MvcResult created =
                mockMvc.perform(
                                post("/api/users/public/webhooks")
                                        .header("X-User-Id", USER_ID)
                                        .header("X-Org-Id", orgId.toString())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(body)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.endpoint.name", is("match-hooks")))
                        .andExpect(
                                jsonPath(
                                        "$.endpoint.targetUrl",
                                        is("https://example.com/hooks/tennisly")))
                        .andExpect(jsonPath("$.endpoint.organizationId", is(orgId.toString())))
                        .andExpect(jsonPath("$.endpoint.active", is(true)))
                        .andExpect(jsonPath("$.plaintextSecret", startsWith("whsec_")))
                        .andExpect(jsonPath("$.plaintextSecret", not(is(""))))
                        .andReturn();

        String endpointId =
                objectMapper
                        .readTree(created.getResponse().getContentAsString())
                        .path("endpoint")
                        .path("id")
                        .asText();

        mockMvc.perform(
                        get("/api/users/public/webhooks")
                                .header("X-User-Id", USER_ID)
                                .header("X-Org-Id", orgId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(endpointId)))
                .andExpect(jsonPath("$[0].name", is("match-hooks")));
    }

    @Test
    void createRequiresUserHeader() throws Exception {
        var body =
                new CreateWebhookEndpointRequest(
                        null,
                        "no-user",
                        "https://example.com/hooks/tennisly",
                        List.of(WebhookEventTypes.MATCH_COMPLETED),
                        null);
        mockMvc.perform(
                        post("/api/users/public/webhooks")
                                .header("X-Org-Id", orgId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createRequiresOrgHeader() throws Exception {
        var body =
                new CreateWebhookEndpointRequest(
                        null,
                        "no-org",
                        "https://example.com/hooks/tennisly",
                        List.of(WebhookEventTypes.MATCH_COMPLETED),
                        null);
        mockMvc.perform(
                        post("/api/users/public/webhooks")
                                .header("X-User-Id", USER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }
}
