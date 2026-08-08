package dev.sahilbasumatary.notificationservice.email;

import dev.sahilbasumatary.notificationservice.client.UserServiceNotificationClient;
import dev.sahilbasumatary.notificationservice.client.dto.EmailPreferenceResponse;
import dev.sahilbasumatary.notificationservice.client.dto.EmailRecipientResponse;
import dev.sahilbasumatary.notificationservice.config.EmailProperties;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

@Service
public class EmailDispatchService {

    private static final Logger log = LoggerFactory.getLogger(EmailDispatchService.class);

    private final EmailSender emailSender;
    private final EmailDeliveryRepository deliveryRepository;
    private final UserServiceNotificationClient notificationClient;
    private final EmailProperties emailProperties;

    public EmailDispatchService(
            EmailSender emailSender,
            EmailDeliveryRepository deliveryRepository,
            UserServiceNotificationClient notificationClient,
            EmailProperties emailProperties) {
        this.emailSender = emailSender;
        this.deliveryRepository = deliveryRepository;
        this.notificationClient = notificationClient;
        this.emailProperties = emailProperties;
    }

    @Transactional
    public void dispatchForClerk(
            String category,
            String eventId,
            String clerkId,
            Supplier<EmailMessage> messageFactory) {
        if (!emailProperties.enabled()) {
            return;
        }
        EmailPreferenceResponse preference;
        try {
            preference = notificationClient.getEmailPreference(clerkId, category);
        } catch (RestClientException ex) {
            log.warn(
                    "Preference lookup failed clerkId={} category={}: {}",
                    clerkId,
                    category,
                    ex.getMessage());
            return;
        }
        if (preference == null
                || preference.email() == null
                || preference.email().isBlank()
                || !preference.enabled()) {
            recordSkipped(
                    category,
                    eventId,
                    preference == null ? null : preference.email(),
                    clerkId,
                    "preference_disabled_or_missing_email");
            return;
        }
        EmailMessage message = messageFactory.get();
        sendAndRecord(category, eventId, preference.email(), clerkId, message);
    }

    @Transactional
    public void dispatchForOrganization(
            String category,
            String eventId,
            UUID organizationId,
            java.util.function.Function<EmailRecipientResponse, EmailMessage> messageFactory) {
        if (!emailProperties.enabled()) {
            return;
        }
        List<EmailRecipientResponse> recipients;
        try {
            recipients = notificationClient.listOrgEmailRecipients(organizationId, category);
        } catch (RestClientException ex) {
            log.warn(
                    "Org recipient lookup failed orgId={} category={}: {}",
                    organizationId,
                    category,
                    ex.getMessage());
            return;
        }
        if (recipients == null || recipients.isEmpty()) {
            return;
        }
        for (EmailRecipientResponse recipient : recipients) {
            EmailMessage message = messageFactory.apply(recipient);
            sendAndRecord(
                    category, eventId, recipient.email(), recipient.clerkId(), message);
        }
    }

    private void sendAndRecord(
            String category, String eventId, String email, String clerkId, EmailMessage message) {
        String idempotencyKey = category + ":" + eventId + ":" + email.toLowerCase(Locale.ROOT);
        if (deliveryRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }
        EmailDelivery delivery = new EmailDelivery();
        delivery.setIdempotencyKey(idempotencyKey);
        delivery.setCategory(category);
        delivery.setEventId(eventId);
        delivery.setRecipientEmail(email);
        delivery.setRecipientClerkId(clerkId);
        delivery.setSubject(message.subject());
        delivery.setProvider(resolvedProviderName());
        try {
            emailSender.send(message);
            delivery.setStatus(EmailDeliveryStatus.SENT);
            deliveryRepository.save(delivery);
        } catch (Exception ex) {
            delivery.setStatus(EmailDeliveryStatus.FAILED);
            delivery.setErrorMessage(truncate(ex.getMessage()));
            deliveryRepository.save(delivery);
            log.error(
                    "Email send failed category={} email={} error={}",
                    category,
                    email,
                    ex.getMessage());
        }
    }

    private void recordSkipped(
            String category, String eventId, String email, String clerkId, String reason) {
        if (email == null || email.isBlank()) {
            return;
        }
        String idempotencyKey = category + ":" + eventId + ":" + email.toLowerCase(Locale.ROOT);
        if (deliveryRepository.existsByIdempotencyKey(idempotencyKey)) {
            return;
        }
        EmailDelivery delivery = new EmailDelivery();
        delivery.setIdempotencyKey(idempotencyKey);
        delivery.setCategory(category);
        delivery.setEventId(eventId);
        delivery.setRecipientEmail(email);
        delivery.setRecipientClerkId(clerkId);
        delivery.setSubject("(skipped)");
        delivery.setStatus(EmailDeliveryStatus.SKIPPED);
        delivery.setProvider(resolvedProviderName());
        delivery.setErrorMessage(reason);
        deliveryRepository.save(delivery);
    }

    private String resolvedProviderName() {
        String provider =
                emailProperties.provider() == null ? "logging" : emailProperties.provider().trim();
        if ("resend".equalsIgnoreCase(provider)
                && emailProperties.resendApiKey() != null
                && !emailProperties.resendApiKey().isBlank()) {
            return "resend";
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
