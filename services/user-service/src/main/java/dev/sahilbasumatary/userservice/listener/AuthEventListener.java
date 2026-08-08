package dev.sahilbasumatary.userservice.listener;

import dev.sahilbasumatary.common.event.OrganizationEvent;
import dev.sahilbasumatary.common.event.UserEvent;
import dev.sahilbasumatary.common.kafka.TopicNames;
import dev.sahilbasumatary.userservice.entity.Organization;
import dev.sahilbasumatary.userservice.entity.UserProfile;
import dev.sahilbasumatary.userservice.repository.OrganizationRepository;
import dev.sahilbasumatary.userservice.repository.UserProfileRepository;
import dev.sahilbasumatary.userservice.service.UserPreferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AuthEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuthEventListener.class);
    private final UserProfileRepository profileRepository;
    private final OrganizationRepository organizationRepository;
    private final UserPreferenceService preferenceService;

    public AuthEventListener(
            UserProfileRepository profileRepository,
            OrganizationRepository organizationRepository,
            UserPreferenceService preferenceService) {
        this.profileRepository = profileRepository;
        this.organizationRepository = organizationRepository;
        this.preferenceService = preferenceService;
    }

    @Transactional
    @KafkaListener(topics = TopicNames.USER_EVENTS, groupId = "${spring.kafka.consumer.group-id}")
    public void handleUserEvent(UserEvent event) {
        log.info("Received user event: eventId={} type={} clerkId={}",
                event.getEventId(), event.getEventType(), event.getClerkId());
        switch (event.getEventType()) {
            case "USER_CREATED" -> onUserCreated(event);
            case "USER_UPDATED" -> onUserUpdated(event);
            case "USER_DELETED" -> onUserDeleted(event);
            default -> log.warn("Unknown user event type: {}", event.getEventType());
        }
    }

    @Transactional
    @KafkaListener(topics = TopicNames.ORGANIZATION_EVENTS, groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrganizationEvent(OrganizationEvent event) {
        log.info("Received org event: eventId={} type={} clerkOrgId={}",
                event.getEventId(), event.getEventType(), event.getClerkOrgId());
        switch (event.getEventType()) {
            case "ORGANIZATION_CREATED" -> onOrganizationCreated(event);
            case "ORGANIZATION_UPDATED" -> onOrganizationUpdated(event);
            case "ORGANIZATION_DELETED" -> onOrganizationDeleted(event);
            default -> log.warn("Unknown org event type: {}", event.getEventType());
        }
    }

    private void onUserCreated(UserEvent event) {
        if (profileRepository.findByClerkId(event.getClerkId()).isPresent()) {
            log.info("Profile already exists for clerkId={}, skipping", event.getClerkId());
            return;
        }
        UserProfile profile = new UserProfile();
        profile.setClerkId(event.getClerkId());
        profile.setEmail(event.getEmail());
        profile.setFirstName(event.getFirstName());
        profile.setLastName(event.getLastName());
        profile.setAvatarUrl(event.getImageUrl());
        profile.setActive(true);
        profileRepository.save(profile);
        preferenceService.ensureDefaults(profile);
        log.info("Created user profile from event for clerkId={}", event.getClerkId());
    }

    private void onUserUpdated(UserEvent event) {
        profileRepository.findByClerkId(event.getClerkId()).ifPresentOrElse(
                profile -> {
                    if (event.getEmail() != null) profile.setEmail(event.getEmail());
                    if (event.getFirstName() != null) profile.setFirstName(event.getFirstName());
                    if (event.getLastName() != null) profile.setLastName(event.getLastName());
                    if (event.getImageUrl() != null) profile.setAvatarUrl(event.getImageUrl());
                    profileRepository.save(profile);
                    log.info("Updated profile from event for clerkId={}", event.getClerkId());
                },
                () -> {
                    log.warn("No profile found for clerkId={}, creating from update event",
                            event.getClerkId());
                    onUserCreated(event);
                }
        );
    }

    private void onUserDeleted(UserEvent event) {
        profileRepository.findByClerkId(event.getClerkId()).ifPresentOrElse(
                profile -> {
                    profile.setActive(false);
                    profileRepository.save(profile);
                    log.info("Deactivated profile from event for clerkId={}",
                            event.getClerkId());
                },
                () -> log.warn("Delete event for unknown profile clerkId={}",
                        event.getClerkId())
        );
    }

    private void onOrganizationCreated(OrganizationEvent event) {
        if (organizationRepository.findByClerkOrgId(event.getClerkOrgId()).isPresent()) {
            log.info("Organization already exists for clerkOrgId={}, skipping",
                    event.getClerkOrgId());
            return;
        }
        Organization org = new Organization();
        org.setClerkOrgId(event.getClerkOrgId());
        org.setName(event.getName());
        org.setSlug(event.getSlug());
        org.setLogoUrl(event.getImageUrl());
        org.setActive(true);
        organizationRepository.save(org);
        log.info("Created organization from event for clerkOrgId={}",
                event.getClerkOrgId());
    }

    private void onOrganizationUpdated(OrganizationEvent event) {
        organizationRepository.findByClerkOrgId(event.getClerkOrgId()).ifPresentOrElse(
                org -> {
                    if (event.getName() != null) org.setName(event.getName());
                    if (event.getSlug() != null) org.setSlug(event.getSlug());
                    if (event.getImageUrl() != null) org.setLogoUrl(event.getImageUrl());
                    organizationRepository.save(org);
                    log.info("Updated org from event for clerkOrgId={}",
                            event.getClerkOrgId());
                },
                () -> {
                    log.warn("No org found for clerkOrgId={}, creating from update event",
                            event.getClerkOrgId());
                    onOrganizationCreated(event);
                }
        );
    }

    private void onOrganizationDeleted(OrganizationEvent event) {
        organizationRepository.findByClerkOrgId(event.getClerkOrgId()).ifPresentOrElse(
                org -> {
                    org.setActive(false);
                    organizationRepository.save(org);
                    log.info("Deactivated org from event for clerkOrgId={}",
                            event.getClerkOrgId());
                },
                () -> log.warn("Delete event for unknown org clerkOrgId={}",
                        event.getClerkOrgId())
        );
    }
}
