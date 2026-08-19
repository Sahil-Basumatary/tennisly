package dev.sahilbasumatary.replayservice.config;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class S3ClientConfig {

    @Bean
    S3Client s3Client(ReplayStorageProperties properties) {
        S3ClientBuilder builder =
                S3Client.builder()
                        .region(Region.of(properties.region()))
                        // R2 rejects AWS chunked encoding and flexible checksums; MinIO accepts these flags too.
                        .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                        .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                        .serviceConfiguration(
                                S3Configuration.builder()
                                        .pathStyleAccessEnabled(properties.pathStyleAccess())
                                        .chunkedEncodingEnabled(false)
                                        .build());
        if (StringUtils.hasText(properties.endpoint())) {
            builder.endpointOverride(URI.create(properties.endpoint()));
        }
        if (StringUtils.hasText(properties.accessKey())
                && StringUtils.hasText(properties.secretKey())) {
            builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(
                                    properties.accessKey(), properties.secretKey())));
        }
        return builder.build();
    }
}
