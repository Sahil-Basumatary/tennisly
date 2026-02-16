package dev.sahilbasumatary.authservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sahilbasumatary.authservice.dto.ClerkEmailAddress;
import dev.sahilbasumatary.authservice.dto.ClerkOrganizationData;
import dev.sahilbasumatary.authservice.dto.ClerkUserData;
import dev.sahilbasumatary.authservice.entity.AppUser;
import dev.sahilbasumatary.authservice.entity.Organization;
import dev.sahilbasumatary.authservice.entity.UserRole;
import dev.sahilbasumatary.authservice.repository.AppUserRepository;
import dev.sahilbasumatary.authservice.repository.OrganizationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebhookService {

    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private final AppUserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final ObjectMapper objectMapper;

    public WebhookService(AppUserRepository userRepository,
            OrganizationRepository organizationRepository,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.objectMapper = objectMapper;
    }

    public void processEvent(String eventType, JsonNode data) {
        switch (eventType) {
            case "user.created" -> handleUserCreated(data);
            case "user.updated" -> handleUserUpdated(data);
            case "user.deleted" -> handleUserDeleted(data);
            case "organization.created" -> handleOrganizationCreated(data);
            case "organization.updated" -> handleOrganizationUpdated(data);
            case "organization.deleted" -> handleOrganizationDeleted(data);
            default -> log.info("Ignoring unhandled webhook event: {}", eventType);
        }
    }

    @Transactional
    void handleUserCreated(JsonNode data) {
        ClerkUserData userData = objectMapper.convertValue(data, ClerkUserData.class);
        // Idempotency: skip if user already synced
        if (userRepository.findByClerkId(userData.id()).isPresent()) {
            log.info("User already exists for clerkId={}, skipping", userData.id());
            return;
        }
        AppUser user = new AppUser();
        user.setClerkId(userData.id());
        user.setEmail(extractPrimaryEmail(userData));
        user.setFirstName(userData.firstName());
        user.setLastName(userData.lastName());
        user.setImageUrl(userData.imageUrl());
        user.setRole(UserRole.USER);
        user.setActive(true);
        userRepository.save(user);
        log.info("Synced new user: clerkId={}", userData.id());
    }

    @Transactional
    void handleUserUpdated(JsonNode data) {
        ClerkUserData userData = objectMapper.convertValue(data, ClerkUserData.class);
        AppUser user = userRepository.findByClerkId(userData.id())
                .orElseGet(() -> {
                    // Clerk can fire updated before created reaches us
                    log.warn("User not found for clerkId={}, creating", userData.id());
                    AppUser newUser = new AppUser();
                    newUser.setClerkId(userData.id());
                    newUser.setRole(UserRole.USER);
                    newUser.setActive(true);
                    return newUser;
                });
        user.setEmail(extractPrimaryEmail(userData));
        user.setFirstName(userData.firstName());
        user.setLastName(userData.lastName());
        user.setImageUrl(userData.imageUrl());
        userRepository.save(user);
        log.info("Updated user: clerkId={}", userData.id());
    }

    @Transactional
    void handleUserDeleted(JsonNode data) {
        String clerkId = data.path("id").asText();
        userRepository.findByClerkId(clerkId).ifPresentOrElse(
                user -> {
                    user.setActive(false);
                    userRepository.save(user);
                    log.info("Soft-deleted user: clerkId={}", clerkId);
                },
                () -> log.warn("Delete event for unknown user: clerkId={}", clerkId)
        );
    }

    @Transactional
    void handleOrganizationCreated(JsonNode data) {
        ClerkOrganizationData orgData =
                objectMapper.convertValue(data, ClerkOrganizationData.class);
        if (organizationRepository.findByClerkOrgId(orgData.id()).isPresent()) {
            log.info("Organization already exists for clerkOrgId={}, skipping",
                    orgData.id());
            return;
        }
        Organization org = new Organization();
        org.setClerkOrgId(orgData.id());
        org.setName(orgData.name());
        org.setSlug(orgData.slug());
        org.setImageUrl(orgData.imageUrl());
        org.setActive(true);
        organizationRepository.save(org);
        log.info("Synced new organization: clerkOrgId={}", orgData.id());
    }

    @Transactional
    void handleOrganizationUpdated(JsonNode data) {
        ClerkOrganizationData orgData =
                objectMapper.convertValue(data, ClerkOrganizationData.class);
        Organization org = organizationRepository.findByClerkOrgId(orgData.id())
                .orElseGet(() -> {
                    log.warn("Organization not found for clerkOrgId={}, creating",
                            orgData.id());
                    Organization newOrg = new Organization();
                    newOrg.setClerkOrgId(orgData.id());
                    newOrg.setActive(true);
                    return newOrg;
                });
        org.setName(orgData.name());
        org.setSlug(orgData.slug());
        org.setImageUrl(orgData.imageUrl());
        organizationRepository.save(org);
        log.info("Updated organization: clerkOrgId={}", orgData.id());
    }

    @Transactional
    void handleOrganizationDeleted(JsonNode data) {
        String clerkOrgId = data.path("id").asText();
        organizationRepository.findByClerkOrgId(clerkOrgId).ifPresentOrElse(
                org -> {
                    org.setActive(false);
                    organizationRepository.save(org);
                    log.info("Soft-deleted organization: clerkOrgId={}", clerkOrgId);
                },
                () -> log.warn("Delete event for unknown org: clerkOrgId={}",
                        clerkOrgId)
        );
    }

    private String extractPrimaryEmail(ClerkUserData userData) {
        if (userData.emailAddresses() == null || userData.emailAddresses().isEmpty()) {
            return null;
        }
        if (userData.primaryEmailAddressId() != null) {
            return userData.emailAddresses().stream()
                    .filter(e -> userData.primaryEmailAddressId().equals(e.id()))
                    .map(ClerkEmailAddress::emailAddress)
                    .findFirst()
                    .orElse(userData.emailAddresses().get(0).emailAddress());
        }
        return userData.emailAddresses().get(0).emailAddress();
    }
}
