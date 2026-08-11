package dev.sahilbasumatary.common.kafka;

import dev.sahilbasumatary.common.event.BaseEvent;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);
    private final KafkaTemplate<String, BaseEvent> kafkaTemplate;

    public EventPublisher(KafkaTemplate<String, BaseEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /** Phase 8a cloud cut — no broker; sync/ingest must not block on metadata timeouts. */
    public static EventPublisher noop() {
        return new EventPublisher(null);
    }

    public void publish(String topic, String key, BaseEvent event) {
        if (kafkaTemplate == null) {
            log.debug(
                    "Kafka disabled — skipping publish eventId={} topic={}",
                    event.getEventId(),
                    topic);
            return;
        }
        ProducerRecord<String, BaseEvent> record = new ProducerRecord<>(topic, key, event);
        record.headers().add("eventType", event.getEventType().getBytes());
        record.headers().add("source", event.getSource().getBytes());
        CompletableFuture<SendResult<String, BaseEvent>> future =
                kafkaTemplate.send(record);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event eventId={} to topic={}: {}",
                        event.getEventId(), topic, ex.getMessage(), ex);
            } else {
                log.info("Published event eventId={} type={} to topic={} partition={} offset={}",
                        event.getEventId(),
                        event.getEventType(),
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
