package dev.sahilbasumatary.notificationservice.controller;

import dev.sahilbasumatary.common.event.MatchEvent;
import dev.sahilbasumatary.common.event.UserEvent;
import dev.sahilbasumatary.common.event.WebhookDomainEvent;
import dev.sahilbasumatary.notificationservice.service.NotificationEventHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/events")
public class InternalNotificationEventController {

    private final NotificationEventHandler notificationEventHandler;

    public InternalNotificationEventController(NotificationEventHandler notificationEventHandler) {
        this.notificationEventHandler = notificationEventHandler;
    }

    @PostMapping("/webhooks")
    public ResponseEntity<Void> ingestWebhook(@RequestBody WebhookDomainEvent event) {
        notificationEventHandler.handleWebhook(event);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/matches")
    public ResponseEntity<Void> ingestMatch(@RequestBody MatchEvent event) {
        notificationEventHandler.handleMatch(event);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/users")
    public ResponseEntity<Void> ingestUser(@RequestBody UserEvent event) {
        notificationEventHandler.handleUser(event);
        return ResponseEntity.noContent().build();
    }
}
