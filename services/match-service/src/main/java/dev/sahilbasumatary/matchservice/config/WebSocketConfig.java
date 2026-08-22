package dev.sahilbasumatary.matchservice.config;

import dev.sahilbasumatary.matchservice.metrics.MatchTimers;
import dev.sahilbasumatary.matchservice.websocket.MatchLiveSessionDecoratorFactory;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
@EnableConfigurationProperties(LiveWebSocketProperties.class)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final LiveWebSocketProperties properties;
    private final MatchTimers matchTimers;
    private final MeterRegistry meterRegistry;

    public WebSocketConfig(
            LiveWebSocketProperties properties, MatchTimers matchTimers, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.matchTimers = matchTimers;
        this.meterRegistry = meterRegistry;
    }

    @Bean
    ThreadPoolTaskScheduler matchBrokerScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("match-broker-");
        scheduler.initialize();
        return scheduler;
    }

    @Bean(name = "matchLiveOutboundExecutor")
    ThreadPoolTaskExecutor matchLiveOutboundExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getOutboundPoolSize());
        executor.setMaxPoolSize(properties.getOutboundPoolSize());
        executor.setQueueCapacity(properties.getOutboundQueueCapacity());
        executor.setThreadNamePrefix("match-ws-out-");
        executor.setRejectedExecutionHandler(
                (runnable, pool) -> {
                    matchTimers.liveOutboundRejected().increment();
                    new ThreadPoolExecutor.AbortPolicy().rejectedExecution(runnable, pool);
                });
        executor.initialize();
        matchTimers.bindOutboundExecutor(executor, meterRegistry);
        return executor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(properties.getHeartbeatMs())
                .setTaskScheduler(matchBrokerScheduler());
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor(matchLiveOutboundExecutor());
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setSendTimeLimit(properties.getSendTimeLimitMs());
        registration.setSendBufferSizeLimit(properties.getSendBufferLimitBytes());
        registration.setDecoratorFactories(new MatchLiveSessionDecoratorFactory(matchTimers));
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/matches").setAllowedOrigins(properties.getAllowedOrigins());
    }
}
