package dev.sahilbasumatary.apigateway.config;

import io.netty.channel.ChannelOption;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties({ApiKeyAuthProperties.class, PlanTierRateLimitProperties.class})
public class UserServiceClientConfig {

    @Bean
    @LoadBalanced
    @ConditionalOnProperty(
            name = "eureka.client.enabled",
            havingValue = "true",
            matchIfMissing = true)
    WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    /**
     * Without a registry the URI is a real host, and the load-balancer filter would try to resolve
     * it as a service id and fail every API-key validation.
     */
    @Bean
    @ConditionalOnProperty(name = "eureka.client.enabled", havingValue = "false")
    WebClient.Builder directWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    WebClient userServiceWebClient(
            WebClient.Builder builder, ApiKeyAuthProperties properties) {
        HttpClient httpClient =
                HttpClient.create()
                        .responseTimeout(properties.getReadTimeout())
                        .option(
                                ChannelOption.CONNECT_TIMEOUT_MILLIS,
                                (int) properties.getConnectTimeout().toMillis());
        return builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(properties.getUserServiceUri())
                .build();
    }
}
