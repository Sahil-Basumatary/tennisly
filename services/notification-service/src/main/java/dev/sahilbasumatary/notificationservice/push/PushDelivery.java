package dev.sahilbasumatary.notificationservice.push;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "push_deliveries")
public class PushDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 200)
    private String idempotencyKey;

    @Column(nullable = false, length = 64)
    private String category;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "recipient_clerk_id", nullable = false, length = 255)
    private String recipientClerkId;

    @Column(name = "device_token_id")
    private UUID deviceTokenId;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PushDeliveryStatus status;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getRecipientClerkId() {
        return recipientClerkId;
    }

    public void setRecipientClerkId(String recipientClerkId) {
        this.recipientClerkId = recipientClerkId;
    }

    public UUID getDeviceTokenId() {
        return deviceTokenId;
    }

    public void setDeviceTokenId(UUID deviceTokenId) {
        this.deviceTokenId = deviceTokenId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public PushDeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(PushDeliveryStatus status) {
        this.status = status;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
