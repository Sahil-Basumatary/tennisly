package dev.sahilbasumatary.authservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.sahilbasumatary.authservice.client.UserProjectionClient;
import dev.sahilbasumatary.authservice.dto.ClerkEmailAddress;
import dev.sahilbasumatary.authservice.dto.ClerkOrganizationData;
import dev.sahilbasumatary.authservice.dto.ClerkUserData;
import dev.sahilbasumatary.authservice.entity.AppUser;
import dev.sahilbasumatary.authservice.entity.Organization;
import dev.sahilbasumatary.authservice.entity.UserRole;
import dev.sahilbasumatary.authservice.repository.AppUserRepository;
import dev.sahilbasumatary.authservice.repository.OrganizationRepository;
import dev.sahilbasumatary.common.event.OrganizationEvent;
import dev.sahilbasumatary.common.event.UserEvent;
import dev.sahilbasumatary.common.kafka.EventPublisher;
import dev.sahilbasumatary.common.kafka.TopicNames;
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
    private final EventPublisher eventPublisher;
    private final UserProjectionClient userProjectionClient;

    public WebhookService(
            AppUserRepository userRepository,
            OrganizationRepository organizationRepository,
            ObjectMapper objectMapper,
            EventPublisher eventPublisher,
            UserProjectionClient userProjectionClient) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.userProjectionClient = userProjectionClient;
    }

    public void processEvent(String eventType, JsonNode data) {
        switch (eventType) {
            case "user.created" -> emitUser(handleUserCreated(data));
            case "user.updated" -> emitUser(handleUserUpdated(data));
            case "user.deleted" -> emitUser(handleUserDeleted(data));
            case "organization.created" -> emitOrg(handleOrganizationCreated(data));
            case "organization.updated" -> emitOrg(handleOrganizationUpdated(data));
            case "organization.deleted" -> emitOrg(handleOrganizationDeleted(data));
            default -> log.info("Ignoring unhandled webhook event: {}", eventType);
        }
    }

    private void emitUser(UserEvent event) {
        if (event == null) {
            return;
        }
        eventPublisher.publish(TopicNames.USER_EVENTS, event.getClerkId(), event);
        userProjectionClient.relayUser(event);
    }

    private void emitOrg(OrganizationEvent event) {
        if (event == null) {
            return;
        }
        eventPublisher.publish(TopicNames.ORGANIZATION_EVENTS, event.getClerkOrgId(), event);
        userProjectionClient.relayOrganization(event);
    }

    @Transactional
    UserEvent handleUserCreated(JsonNode data) {
        ClerkUserData userData = objectMapper.convertValue(data, ClerkUserData.class);
        String email = extractPrimaryEmail(userData);
        // Phone-only Clerk accounts (and dashboard test payloads) carry no email, and
        // email is NOT NULL — ack the delivery instead of making Clerk retry forever.
        if (email == null || email.isBlank()) {
            log.warn("Skipping user without email: clerkId={}", userData.id());
            return null;
        }
        if (userRepository.findByClerkId(userData.id()).isEmpty()) {
            AppUser user = new AppUser();
            user.setClerkId(userData.id());
            user.setEmail(email);
            user.setFirstName(userData.firstName());
            user.setLastName(userData.lastName());
            user.setImageUrl(userData.imageUrl());
            user.setRole(UserRole.USER);
            user.setActive(true);
            userRepository.save(user);
            log.info("Synced new user: clerkId={}", userData.id());
        } else {
            log.info("User already exists for clerkId={}, still projecting", userData.id());
        }
        return UserEvent.created(
                userData.id(),
                email,
                userData.firstName(),
                userData.lastName(),
                userData.imageUrl());
    }

    @Transactional
    UserEvent handleUserUpdated(JsonNode data) {
        ClerkUserData userData = objectMapper.convertValue(data, ClerkUserData.class);
        AppUser user = userRepository.findByClerkId(userData.id())
                .orElseGet(() -> {
                    log.warn("User not found for clerkId={}, creating", userData.id());
                    AppUser newUser = new AppUser();
                    newUser.setClerkId(userData.id());
                    newUser.setRole(UserRole.USER);
                    newUser.setActive(true);
                    return newUser;
                });
        String email = extractPrimaryEmail(userData);
        if (email == null || email.isBlank()) {
            if (user.getEmail() == null) {
                log.warn("Skipping user update without email: clerkId={}", userData.id());
                return null;
            }
            email = user.getEmail();
        }
        user.setEmail(email);
        user.setFirstName(userData.firstName());
        user.setLastName(userData.lastName());
        user.setImageUrl(userData.imageUrl());
        userRepository.save(user);
        log.info("Updated user: clerkId={}", userData.id());
        return UserEvent.updated(
                userData.id(),
                email,
                userData.firstName(),
                userData.lastName(),
                userData.imageUrl());
    }

    @Transactional
    UserEvent handleUserDeleted(JsonNode data) {
        String clerkId = data.path("id").asText();
        return userRepository.findByClerkId(clerkId).map(user -> {
            user.setActive(false);
            userRepository.save(user);
            log.info("Soft-deleted user: clerkId={}", clerkId);
            return UserEvent.deleted(clerkId);
        }).orElseGet(() -> {
            log.warn("Delete event for unknown user: clerkId={}", clerkId);
            return null;
        });
    }

    @Transactional
    OrganizationEvent handleOrganizationCreated(JsonNode data) {
        ClerkOrganizationData orgData =
                objectMapper.convertValue(data, ClerkOrganizationData.class);
        if (organizationRepository.findByClerkOrgId(orgData.id()).isEmpty()) {
            Organization org = new Organization();
            org.setClerkOrgId(orgData.id());
            org.setName(orgData.name());
            org.setSlug(orgData.slug());
            org.setImageUrl(orgData.imageUrl());
            org.setActive(true);
            organizationRepository.save(org);
            log.info("Synced new organization: clerkOrgId={}", orgData.id());
        } else {
            log.info("Organization already exists for clerkOrgId={}, still projecting",
                    orgData.id());
        }
        return OrganizationEvent.created(
                orgData.id(), orgData.name(), orgData.slug(), orgData.imageUrl());
    }

    @Transactional
    OrganizationEvent handleOrganizationUpdated(JsonNode data) {
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
        return OrganizationEvent.updated(
                orgData.id(), orgData.name(), orgData.slug(), orgData.imageUrl());
    }

    @Transactional
    OrganizationEvent handleOrganizationDeleted(JsonNode data) {
        String clerkOrgId = data.path("id").asText();
        return organizationRepository.findByClerkOrgId(clerkOrgId).map(org -> {
            org.setActive(false);
            organizationRepository.save(org);
            log.info("Soft-deleted organization: clerkOrgId={}", clerkOrgId);
            return OrganizationEvent.deleted(clerkOrgId);
        }).orElseGet(() -> {
            log.warn("Delete event for unknown org: clerkOrgId={}", clerkOrgId);
            return null;
        });
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
