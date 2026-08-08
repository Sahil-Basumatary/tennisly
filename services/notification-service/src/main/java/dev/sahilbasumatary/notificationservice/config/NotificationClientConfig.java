package dev.sahilbasumatary.notificationservice.config;

import java.net.URI;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class NotificationClientConfig {

    @Bean
    @LoadBalanced
    RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    RestClient userServiceRestClient(
            @LoadBalanced RestClient.Builder loadBalancedBuilder,
            NotificationClientProperties properties) {
        String uri = properties.userServiceUri();
        RestClient.Builder builder =
                usesDiscovery(uri) ? loadBalancedBuilder.clone() : RestClient.builder();
        return builder.baseUrl(uri).requestFactory(requestFactory(properties)).build();
    }

    private static boolean usesDiscovery(String uri) {
        return URI.create(uri).getPort() == -1;
    }

    private ClientHttpRequestFactory requestFactory(NotificationClientProperties properties) {
        ClientHttpRequestFactorySettings settings =
                ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(properties.connectTimeout())
                        .withReadTimeout(properties.readTimeout());
        return ClientHttpRequestFactories.get(settings);
    }
}
