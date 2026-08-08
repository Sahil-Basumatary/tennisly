package dev.sahilbasumatary.analyticsservice.config;

import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.AliasAction;
import org.springframework.data.elasticsearch.core.index.AliasActionParameters;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.index.AliasData;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;

@Component
public class ElasticsearchIndexBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchIndexBootstrap.class);

    private final AnalyticsElasticsearchProperties properties;
    private final ElasticsearchOperations elasticsearchOperations;

    public ElasticsearchIndexBootstrap(
            AnalyticsElasticsearchProperties properties,
            ElasticsearchOperations elasticsearchOperations) {
        this.properties = properties;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info(
                "Elasticsearch configured: matchIndex={} matchAlias={} playerMatchIndex={} playerMatchAlias={}",
                properties.matchIndex(),
                properties.matchAlias(),
                properties.playerMatchIndex(),
                properties.playerMatchAlias());
        ensureIndex(properties.matchIndex(), properties.matchAlias(), ElasticsearchMappings.matchAnalyticsMapping());
        ensureIndex(
                properties.playerMatchIndex(),
                properties.playerMatchAlias(),
                ElasticsearchMappings.playerMatchMapping());
    }

    private void ensureIndex(String indexName, String aliasName, Document mapping) {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(IndexCoordinates.of(indexName));
            if (!indexOps.exists()) {
                indexOps.create(Map.of(), mapping);
                log.info("Created Elasticsearch index {}", indexName);
            }
            ensureAlias(indexOps, indexName, aliasName);
        } catch (Exception ex) {
            log.warn("Failed to ensure index {} with alias {}: {}", indexName, aliasName, ex.getMessage());
        }
    }

    private void ensureAlias(IndexOperations indexOps, String indexName, String aliasName) {
        try {
            if (aliasPointsToIndex(indexOps, aliasName)) {
                return;
            }
            AliasActions actions = new AliasActions();
            actions.add(
                    new AliasAction.Add(
                            AliasActionParameters.builder()
                                    .withIndices(indexName)
                                    .withAliases(aliasName)
                                    .build()));
            indexOps.alias(actions);
            log.info("Attached alias {} -> {}", aliasName, indexName);
        } catch (Exception ex) {
            log.warn("Could not attach alias {} -> {}: {}", aliasName, indexName, ex.getMessage());
        }
    }

    private boolean aliasPointsToIndex(IndexOperations indexOps, String aliasName) {
        try {
            Map<String, Set<AliasData>> aliases = indexOps.getAliases();
            return aliases.values().stream()
                    .flatMap(Set::stream)
                    .anyMatch(alias -> aliasName.equals(alias.getAlias()));
        } catch (Exception ex) {
            log.debug("Could not read aliases for index: {}", ex.getMessage());
            return false;
        }
    }
}
