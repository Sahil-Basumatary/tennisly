package dev.sahilbasumatary.matchservice.seed;

import dev.sahilbasumatary.matchservice.client.TennisDataPlayerClient;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CataloguePlayerResolver {

    private static final Logger log = LoggerFactory.getLogger(CataloguePlayerResolver.class);

    private final TennisDataPlayerClient tennisDataPlayerClient;

    public CataloguePlayerResolver(TennisDataPlayerClient tennisDataPlayerClient) {
        this.tennisDataPlayerClient = tennisDataPlayerClient;
    }

    public UUID resolve(String externalId) {
        return tennisDataPlayerClient
                .findPlayerIdByExternalId(externalId)
                .orElseGet(
                        () -> {
                            UUID fallback = CataloguePlayerIdentity.FALLBACK_BY_EXTERNAL.get(externalId);
                            if (fallback == null) {
                                throw new IllegalArgumentException(
                                        "No catalogue fallback for externalId=" + externalId);
                            }
                            log.debug(
                                    "Using catalogue fallback playerId for externalId={}", externalId);
                            return fallback;
                        });
    }
}
