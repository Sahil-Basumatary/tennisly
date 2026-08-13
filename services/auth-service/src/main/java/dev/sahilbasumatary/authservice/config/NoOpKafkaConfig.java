package dev.sahilbasumatary.authservice.config;

import dev.sahilbasumatary.common.kafka.EventPublisher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "tennisly.kafka.enabled", havingValue = "false")
public class NoOpKafkaConfig {

    @Bean
    EventPublisher eventPublisher() {
        return EventPublisher.noop();
    }
}
