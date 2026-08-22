package dev.sahilbasumatary.matchservice.client;

import dev.sahilbasumatary.common.security.InternalToken;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

final class PooledRestClients {

    private PooledRestClients() {}

    static RestClient maybeBuild(String baseUri, String internalToken) {
        if (baseUri == null || baseUri.isBlank()) {
            return null;
        }
        HttpClient http =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(20));
        RestClient.Builder builder =
                RestClient.builder()
                        .baseUrl(baseUri.replaceAll("/$", ""))
                        .requestFactory(factory);
        if (InternalToken.isEnabled(internalToken)) {
            builder.defaultHeader(InternalToken.HEADER, internalToken);
        }
        return builder.build();
    }
}
