package dev.sahilbasumatary.matchservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tennisly.clients")
public class MatchClientProperties {

    /** Direct URI so match ingestion works before Eureka is warm. */
    private String tennisDataServiceUri = "http://localhost:8083";

    public String getTennisDataServiceUri() {
        return tennisDataServiceUri;
    }

    public void setTennisDataServiceUri(String tennisDataServiceUri) {
        this.tennisDataServiceUri = tennisDataServiceUri;
    }
}
