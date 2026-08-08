package dev.sahilbasumatary.apigateway.config;

import io.netty.channel.ChannelOption;
import java.time.Duration;
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
    WebClient.Builder loadBalancedWebClientBuilder() {
        return WebClient.builder();
    }

    @Bean
    WebClient userServiceWebClient(
            @LoadBalanced WebClient.Builder builder, ApiKeyAuthProperties properties) {
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
