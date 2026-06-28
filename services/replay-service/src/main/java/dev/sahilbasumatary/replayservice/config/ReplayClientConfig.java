package dev.sahilbasumatary.replayservice.config;

import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Builds load-balanced {@link RestClient} instances for the downstream tennis services. The builder
 * is cloned per client so each carries its own base URI without mutating the shared bean.
 */
@Configuration
public class ReplayClientConfig {

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    RestClient matchServiceRestClient(
            @LoadBalanced RestClient.Builder builder, ReplayClientProperties properties) {
        return builder.clone()
                .baseUrl(properties.matchServiceUri())
                .requestFactory(requestFactory(properties))
                .build();
    }

    @Bean
    RestClient tennisDataServiceRestClient(
            @LoadBalanced RestClient.Builder builder, ReplayClientProperties properties) {
        return builder.clone()
                .baseUrl(properties.tennisDataServiceUri())
                .requestFactory(requestFactory(properties))
                .build();
    }

    private ClientHttpRequestFactory requestFactory(ReplayClientProperties properties) {
        ClientHttpRequestFactorySettings settings =
                ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(properties.connectTimeout())
                        .withReadTimeout(properties.readTimeout());
        return ClientHttpRequestFactories.get(settings);
    }
}
