package dev.sahilbasumatary.notificationservice.push;

import dev.sahilbasumatary.notificationservice.client.UserServiceNotificationClient;
import dev.sahilbasumatary.notificationservice.client.dto.PushPreferenceResponse;
import dev.sahilbasumatary.notificationservice.client.dto.PushRecipientResponse;
import dev.sahilbasumatary.notificationservice.config.PushProperties;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

@Service
public class PushDispatchService {

    private static final Logger log = LoggerFactory.getLogger(PushDispatchService.class);

    private final PushSender pushSender;
    private final PushDeliveryRepository deliveryRepository;
    private final DevicePushTokenRepository tokenRepository;
    private final UserServiceNotificationClient notificationClient;
    private final PushProperties pushProperties;

    public PushDispatchService(
            PushSender pushSender,
            PushDeliveryRepository deliveryRepository,
            DevicePushTokenRepository tokenRepository,
            UserServiceNotificationClient notificationClient,
            PushProperties pushProperties) {
        this.pushSender = pushSender;
        this.deliveryRepository = deliveryRepository;
        this.tokenRepository = tokenRepository;
        this.notificationClient = notificationClient;
        this.pushProperties = pushProperties;
    }

    @Transactional
    public void dispatchForClerk(
            String category, String eventId, String clerkId, Supplier<PushContent> contentFactory) {
        if (!pushProperties.enabled()) {
            return;
        }
        PushPreferenceResponse preference;
        try {
            preference = notificationClient.getPushPreference(clerkId, category);
        } catch (RestClientException ex) {
            log.warn(
                    "Push preference lookup failed clerkId={} category={}: {}",
                    clerkId,
                    category,
                    ex.getMessage());
            return;
        }
        if (preference == null || !preference.enabled()) {
            recordSkipped(category, eventId, clerkId, null, "preference_disabled");
            return;
        }
        PushContent content = contentFactory.get();
        List<DevicePushToken> tokens = tokenRepository.findByClerkIdAndActiveTrue(clerkId);
        if (tokens.isEmpty()) {
            recordSkipped(category, eventId, clerkId, content.title(), "no_device_tokens");
            return;
        }
        for (DevicePushToken token : tokens) {
            sendToToken(category, eventId, clerkId, token, content);
        }
    }

    @Transactional
    public void dispatchForOrganization(
            String category,
            String eventId,
            UUID organizationId,
            Function<PushRecipientResponse, PushContent> contentFactory) {
        if (!pushProperties.enabled()) {
            return;
        }
        List<PushRecipientResponse> recipients;
        try {
            recipients = notificationClient.listOrgPushRecipients(organizationId, category);
        } catch (RestClientException ex) {
            log.warn(
                    "Push org recipient lookup failed orgId={} category={}: {}",
                    organizationId,
                    category,
                    ex.getMessage());
            return;
        }
        if (recipients == null || recipients.isEmpty()) {
            return;
        }
        for (PushRecipientResponse recipient : recipients) {
            PushContent content = contentFactory.apply(recipient);
            List<DevicePushToken> tokens =
                    tokenRepository.findByClerkIdAndActiveTrue(recipient.clerkId());
            if (tokens.isEmpty()) {
                recordSkipped(
                        category, eventId, recipient.clerkId(), content.title(), "no_device_tokens");
                continue;
            }
            for (DevicePushToken token : tokens) {
                sendToToken(category, eventId, recipient.clerkId(), token, content);
            }
        }
    }

    private void sendToToken(
            String category,
            String eventId,
            String clerkId,
            DevicePushToken token,
            PushContent content) {
        String idempotencyKey = category + ":" + eventId + ":" + token.getId();
        if (deliveryRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }
        PushDelivery delivery = new PushDelivery();
        delivery.setIdempotencyKey(idempotencyKey);
        delivery.setCategory(category);
        delivery.setEventId(eventId);
        delivery.setRecipientClerkId(clerkId);
        delivery.setDeviceTokenId(token.getId());
        delivery.setTitle(content.title());
        delivery.setProvider(resolvedProviderName());
        try {
            pushSender.send(
                    new PushMessage(token.getToken(), content.title(), content.body(), content.data()));
            delivery.setStatus(PushDeliveryStatus.SENT);
            deliveryRepository.save(delivery);
        } catch (Exception ex) {
            delivery.setStatus(PushDeliveryStatus.FAILED);
            delivery.setErrorMessage(truncate(ex.getMessage()));
            deliveryRepository.save(delivery);
            log.error(
                    "Push send failed category={} clerkId={} error={}",
                    category,
                    clerkId,
                    ex.getMessage());
        }
    }

    private void recordSkipped(
            String category, String eventId, String clerkId, String title, String reason) {
        String idempotencyKey = category + ":" + eventId + ":" + clerkId + ":skipped";
        if (deliveryRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }
        PushDelivery delivery = new PushDelivery();
        delivery.setIdempotencyKey(idempotencyKey);
        delivery.setCategory(category);
        delivery.setEventId(eventId);
        delivery.setRecipientClerkId(clerkId);
        delivery.setTitle(title == null ? "(skipped)" : title);
        delivery.setStatus(PushDeliveryStatus.SKIPPED);
        delivery.setProvider(resolvedProviderName());
        delivery.setErrorMessage(reason);
        deliveryRepository.save(delivery);
    }

    private String resolvedProviderName() {
        String provider =
                pushProperties.provider() == null ? "logging" : pushProperties.provider().trim();
        if ("fcm".equalsIgnoreCase(provider)
                && pushProperties.fcmProjectId() != null
                && !pushProperties.fcmProjectId().isBlank()
                && pushProperties.fcmAccessToken() != null
                && !pushProperties.fcmAccessToken().isBlank()) {
            return "fcm";
        }
        return "logging";
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 500 ? value : value.substring(0, 500);
    }
}
