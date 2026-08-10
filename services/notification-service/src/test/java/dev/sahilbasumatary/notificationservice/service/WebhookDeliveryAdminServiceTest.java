package dev.sahilbasumatary.notificationservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.sahilbasumatary.notificationservice.context.RequestContext;
import dev.sahilbasumatary.notificationservice.entity.DeliveryStatus;
import dev.sahilbasumatary.notificationservice.entity.WebhookDelivery;
import dev.sahilbasumatary.notificationservice.entity.WebhookDeliveryRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WebhookDeliveryAdminServiceTest {

    @Mock
    private WebhookDeliveryRepository deliveryRepository;

    @InjectMocks
    private WebhookDeliveryAdminService service;

    @BeforeEach
    void setAdmin() {
        RequestContext.setUserId("admin_user");
        RequestContext.setRoles(Set.of("ADMIN"));
    }

    @AfterEach
    void clear() {
        RequestContext.clear();
    }

    @Test
    void retryDeadResetsAttemptBudgetAndQueuesNow() {
        UUID id = UUID.randomUUID();
        WebhookDelivery delivery = baseDelivery(id);
        delivery.setStatus(DeliveryStatus.DEAD);
        delivery.setAttemptCount(5);
        delivery.setLastError("HTTP 500");
        when(deliveryRepository.findById(id)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        final var response = service.retry(id);

        ArgumentCaptor<WebhookDelivery> captor = ArgumentCaptor.forClass(WebhookDelivery.class);
        verify(deliveryRepository).save(captor.capture());
        WebhookDelivery saved = captor.getValue();
        assertEquals(DeliveryStatus.PENDING, saved.getStatus());
        assertEquals(0, saved.getAttemptCount());
        assertNull(saved.getLastError());
        assertEquals(DeliveryStatus.PENDING, response.status());
    }

    @Test
    void retryFailedKeepsAttemptCount() {
        UUID id = UUID.randomUUID();
        WebhookDelivery delivery = baseDelivery(id);
        delivery.setStatus(DeliveryStatus.FAILED);
        delivery.setAttemptCount(2);
        when(deliveryRepository.findById(id)).thenReturn(Optional.of(delivery));
        when(deliveryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.retry(id);

        ArgumentCaptor<WebhookDelivery> captor = ArgumentCaptor.forClass(WebhookDelivery.class);
        verify(deliveryRepository).save(captor.capture());
        assertEquals(2, captor.getValue().getAttemptCount());
        assertEquals(DeliveryStatus.PENDING, captor.getValue().getStatus());
    }

    @Test
    void retrySuccessRejected() {
        UUID id = UUID.randomUUID();
        WebhookDelivery delivery = baseDelivery(id);
        delivery.setStatus(DeliveryStatus.SUCCESS);
        when(deliveryRepository.findById(id)).thenReturn(Optional.of(delivery));

        assertThrows(IllegalStateException.class, () -> service.retry(id));
    }

    private static WebhookDelivery baseDelivery(UUID id) {
        WebhookDelivery delivery = new WebhookDelivery();
        delivery.setId(id);
        delivery.setEndpointId(UUID.randomUUID());
        delivery.setOrganizationId(UUID.randomUUID());
        delivery.setEventId("evt");
        delivery.setEventType("match.completed");
        delivery.setPayload("{}");
        delivery.setIdempotencyKey(id + ":evt");
        delivery.setMaxAttempts(5);
        return delivery;
    }
}
