package dev.sahilbasumatary.notificationservice.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sahilbasumatary.common.notification.EmailCategories;
import dev.sahilbasumatary.notificationservice.client.UserServiceNotificationClient;
import dev.sahilbasumatary.notificationservice.client.dto.EmailPreferenceResponse;
import dev.sahilbasumatary.notificationservice.config.EmailProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailDispatchServiceTest {

    @Mock
    private EmailSender emailSender;

    @Mock
    private EmailDeliveryRepository deliveryRepository;

    @Mock
    private UserServiceNotificationClient notificationClient;

    @Mock
    private EmailProperties emailProperties;

    @InjectMocks
    private EmailDispatchService dispatchService;

    @Test
    void skipsWhenPreferenceDisabled() {
        when(emailProperties.enabled()).thenReturn(true);
        when(emailProperties.provider()).thenReturn("logging");
        when(notificationClient.getEmailPreference("user_1", EmailCategories.WELCOME))
                .thenReturn(
                        new EmailPreferenceResponse("user_1", "a@example.com", "Sahil", false));
        when(deliveryRepository.existsByIdempotencyKey(any())).thenReturn(false);

        dispatchService.dispatchForClerk(
                EmailCategories.WELCOME,
                "evt-1",
                "user_1",
                () -> new EmailMessage("a@example.com", "Welcome", "<p>Hi</p>", "Hi"));

        verify(emailSender, never()).send(any());
        ArgumentCaptor<EmailDelivery> captor = ArgumentCaptor.forClass(EmailDelivery.class);
        verify(deliveryRepository).save(captor.capture());
        assertEquals(EmailDeliveryStatus.SKIPPED, captor.getValue().getStatus());
    }

    @Test
    void sendsWhenPreferenceEnabled() {
        when(emailProperties.enabled()).thenReturn(true);
        when(emailProperties.provider()).thenReturn("logging");
        when(notificationClient.getEmailPreference("user_1", EmailCategories.WELCOME))
                .thenReturn(new EmailPreferenceResponse("user_1", "a@example.com", "Sahil", true));
        when(deliveryRepository.existsByIdempotencyKey(any())).thenReturn(false);

        dispatchService.dispatchForClerk(
                EmailCategories.WELCOME,
                "evt-2",
                "user_1",
                () -> new EmailMessage("a@example.com", "Welcome", "<p>Hi</p>", "Hi"));

        verify(emailSender).send(any());
        ArgumentCaptor<EmailDelivery> captor = ArgumentCaptor.forClass(EmailDelivery.class);
        verify(deliveryRepository).save(captor.capture());
        assertEquals(EmailDeliveryStatus.SENT, captor.getValue().getStatus());
        assertEquals("a@example.com", captor.getValue().getRecipientEmail());
    }
}
