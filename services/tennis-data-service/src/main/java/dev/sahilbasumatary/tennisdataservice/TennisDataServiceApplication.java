package dev.sahilbasumatary.tennisdataservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {
    "dev.sahilbasumatary.tennisdataservice",
    "dev.sahilbasumatary.common.kafka"
})
public class TennisDataServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TennisDataServiceApplication.class, args);
    }
}
