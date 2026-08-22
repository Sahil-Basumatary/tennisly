package dev.sahilbasumatary.matchservice.config;

import dev.sahilbasumatary.matchservice.service.MatchLiveRedisSubscriber;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class RedisLiveEventConfig {

    @Bean(name = "matchLiveRedisListenerExecutor")
    ThreadPoolTaskExecutor matchLiveRedisListenerExecutor() {
        return executor("match-live-listener-", 10_000);
    }

    @Bean(name = "matchLiveRedisSubscriptionExecutor")
    ThreadPoolTaskExecutor matchLiveRedisSubscriptionExecutor() {
        return executor("match-live-subscription-", 1);
    }

    @Bean
    RedisMessageListenerContainer matchLiveMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MatchLiveRedisSubscriber subscriber,
            @Qualifier("matchLiveRedisListenerExecutor") Executor listenerExecutor,
            @Qualifier("matchLiveRedisSubscriptionExecutor") Executor subscriptionExecutor,
            @Value("${tennisly.websocket.redis-channel:match-live-events}") String channel) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setTaskExecutor(listenerExecutor);
        container.setSubscriptionExecutor(subscriptionExecutor);
        container.addMessageListener(subscriber, new ChannelTopic(channel));
        return container;
    }

    private ThreadPoolTaskExecutor executor(String threadPrefix, int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix(threadPrefix);
        executor.initialize();
        return executor;
    }
}
