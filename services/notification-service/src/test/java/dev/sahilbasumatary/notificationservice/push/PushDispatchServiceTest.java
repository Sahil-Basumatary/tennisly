package dev.sahilbasumatary.notificationservice.push;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sahilbasumatary.common.notification.NotificationCategories;
import dev.sahilbasumatary.notificationservice.client.UserServiceNotificationClient;
import dev.sahilbasumatary.notificationservice.client.dto.PushPreferenceResponse;
import dev.sahilbasumatary.notificationservice.config.PushProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PushDispatchServiceTest {

    @Mock
    private PushSender pushSender;

    @Mock
    private PushDeliveryRepository deliveryRepository;

    @Mock
    private DevicePushTokenRepository tokenRepository;

    @Mock
    private UserServiceNotificationClient notificationClient;

    @Mock
    private PushProperties pushProperties;

    @InjectMocks
    private PushDispatchService dispatchService;

    @Test
    void skipsWhenNoDeviceTokens() {
        when(pushProperties.enabled()).thenReturn(true);
        when(pushProperties.provider()).thenReturn("logging");
        when(notificationClient.getPushPreference("user_1", NotificationCategories.WELCOME))
                .thenReturn(new PushPreferenceResponse("user_1", "Sahil", true));
        when(tokenRepository.findByClerkIdAndActiveTrue("user_1")).thenReturn(List.of());
        when(deliveryRepository.existsByIdempotencyKey(any())).thenReturn(false);

        dispatchService.dispatchForClerk(
                NotificationCategories.WELCOME,
                "evt-1",
                "user_1",
                () -> new PushContent("Welcome", "Hi", null));

        verify(pushSender, never()).send(any());
        ArgumentCaptor<PushDelivery> captor = ArgumentCaptor.forClass(PushDelivery.class);
        verify(deliveryRepository).save(captor.capture());
        assertEquals(PushDeliveryStatus.SKIPPED, captor.getValue().getStatus());
    }

    @Test
    void sendsToActiveTokens() {
        when(pushProperties.enabled()).thenReturn(true);
        when(pushProperties.provider()).thenReturn("logging");
        when(notificationClient.getPushPreference("user_1", NotificationCategories.WELCOME))
                .thenReturn(new PushPreferenceResponse("user_1", "Sahil", true));
        DevicePushToken token = new DevicePushToken();
        token.setId(java.util.UUID.randomUUID());
        token.setClerkId("user_1");
        token.setToken("device-token-abc");
        token.setPlatform(PushPlatform.WEB);
        when(tokenRepository.findByClerkIdAndActiveTrue("user_1")).thenReturn(List.of(token));
        when(deliveryRepository.existsByIdempotencyKey(any())).thenReturn(false);

        dispatchService.dispatchForClerk(
                NotificationCategories.WELCOME,
                "evt-2",
                "user_1",
                () -> new PushContent("Welcome", "Hi", null));

        verify(pushSender).send(any());
        ArgumentCaptor<PushDelivery> captor = ArgumentCaptor.forClass(PushDelivery.class);
        verify(deliveryRepository).save(captor.capture());
        assertEquals(PushDeliveryStatus.SENT, captor.getValue().getStatus());
    }
}
