package dev.sahilbasumatary.notificationservice.listener;

import dev.sahilbasumatary.common.event.BaseEvent;
import dev.sahilbasumatary.common.event.MatchEvent;
import dev.sahilbasumatary.common.event.WebhookEventTypes;
import dev.sahilbasumatary.common.kafka.TopicNames;
import dev.sahilbasumatary.notificationservice.service.EnqueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Profile("!it")
public class MatchEventWebhookListener {

    private static final Logger log = LoggerFactory.getLogger(MatchEventWebhookListener.class);

    private final EnqueueService enqueueService;

    public MatchEventWebhookListener(EnqueueService enqueueService) {
        this.enqueueService = enqueueService;
    }

    @KafkaListener(
            topics = TopicNames.MATCH_EVENTS,
            groupId = "notification-service-group",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMatchEvent(BaseEvent baseEvent) {
        if (!(baseEvent instanceof MatchEvent event)) {
            return;
        }
        String webhookType = resolveWebhookType(event);
        if (webhookType == null) {
            return;
        }
        log.info("Routing match event eventId={} type={} -> {}",
                event.getEventId(), event.getEventType(), webhookType);
        enqueueService.enqueue(webhookType, event);
    }

    private String resolveWebhookType(MatchEvent event) {
        if (MatchEvent.MATCH_STATUS_CHANGED.equals(event.getEventType())
                && "COMPLETED".equals(event.getStatus())) {
            return WebhookEventTypes.MATCH_COMPLETED;
        }
        if (MatchEvent.MATCH_POINT_RECORDED.equals(event.getEventType())) {
            return WebhookEventTypes.MATCH_POINT_RECORDED;
        }
        return null;
    }
}
