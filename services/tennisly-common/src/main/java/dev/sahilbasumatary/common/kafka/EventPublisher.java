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

    public void publish(String topic, String key, BaseEvent event) {
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
