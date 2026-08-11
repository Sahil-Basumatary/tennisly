package dev.sahilbasumatary.common.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

// KafkaAdmin materialises every NewTopic bean on refresh, so a brokerless deploy
// retries the bootstrap server forever unless the whole config drops out.
@Configuration
@ConditionalOnProperty(
        name = "tennisly.kafka.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class KafkaTopicConfig {

    private static final int DEFAULT_PARTITIONS = 3;
    private static final int DEFAULT_REPLICAS = 1;
    private static final String RETENTION_MS = "604800000"; // 7 days

    @Bean
    public NewTopic userEventsTopic() {
        return TopicBuilder.name(TopicNames.USER_EVENTS)
                .partitions(DEFAULT_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .config("retention.ms", RETENTION_MS)
                .build();
    }

    @Bean
    public NewTopic organizationEventsTopic() {
        return TopicBuilder.name(TopicNames.ORGANIZATION_EVENTS)
                .partitions(DEFAULT_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .config("retention.ms", RETENTION_MS)
                .build();
    }

    @Bean
    public NewTopic tennisDataEventsTopic() {
        return TopicBuilder.name(TopicNames.TENNIS_DATA_EVENTS)
                .partitions(DEFAULT_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .config("retention.ms", RETENTION_MS)
                .build();
    }

    @Bean
    public NewTopic matchEventsTopic() {
        return TopicBuilder.name(TopicNames.MATCH_EVENTS)
                .partitions(DEFAULT_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .config("retention.ms", RETENTION_MS)
                .build();
    }

    @Bean
    public NewTopic userEventsDlqTopic() {
        return TopicBuilder.name(TopicNames.USER_EVENTS_DLQ)
                .partitions(1)
                .replicas(DEFAULT_REPLICAS)
                .build();
    }

    @Bean
    public NewTopic organizationEventsDlqTopic() {
        return TopicBuilder.name(TopicNames.ORGANIZATION_EVENTS_DLQ)
                .partitions(1)
                .replicas(DEFAULT_REPLICAS)
                .build();
    }

    @Bean
    public NewTopic tennisDataEventsDlqTopic() {
        return TopicBuilder.name(TopicNames.TENNIS_DATA_EVENTS_DLQ)
                .partitions(1)
                .replicas(DEFAULT_REPLICAS)
                .build();
    }

    @Bean
    public NewTopic matchEventsDlqTopic() {
        return TopicBuilder.name(TopicNames.MATCH_EVENTS_DLQ)
                .partitions(1)
                .replicas(DEFAULT_REPLICAS)
                .build();
    }

    @Bean
    public NewTopic webhookEventsTopic() {
        return TopicBuilder.name(TopicNames.WEBHOOK_EVENTS)
                .partitions(DEFAULT_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .config("retention.ms", RETENTION_MS)
                .build();
    }

    @Bean
    public NewTopic webhookEventsDlqTopic() {
        return TopicBuilder.name(TopicNames.WEBHOOK_EVENTS_DLQ)
                .partitions(1)
                .replicas(DEFAULT_REPLICAS)
                .build();
    }
}
