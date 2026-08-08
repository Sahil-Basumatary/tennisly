package dev.sahilbasumatary.analyticsservice.repository;

import dev.sahilbasumatary.analyticsservice.config.AnalyticsElasticsearchProperties;
import dev.sahilbasumatary.analyticsservice.index.MatchAnalyticsDocument;
import dev.sahilbasumatary.analyticsservice.index.PlayerMatchDocument;
import dev.sahilbasumatary.analyticsservice.query.AnalyticsQueryBounds;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Repository;

@Repository
public class AnalyticsReadRepository {

    private static final Sort RECENT_MATCH_SORT =
            Sort.by(Sort.Order.desc("endedAt"), Sort.Order.desc("scheduledAt"), Sort.Order.desc("matchId"));

    private static final Sort CHRONOLOGICAL_SORT =
            Sort.by(Sort.Order.asc("endedAt"), Sort.Order.asc("scheduledAt"), Sort.Order.asc("matchId"));

    private final AnalyticsElasticsearchProperties properties;
    private final ElasticsearchOperations elasticsearchOperations;

    public AnalyticsReadRepository(
            AnalyticsElasticsearchProperties properties,
            ElasticsearchOperations elasticsearchOperations) {
        this.properties = properties;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    public Optional<MatchAnalyticsDocument> findMatchById(UUID matchId) {
        MatchAnalyticsDocument document =
                elasticsearchOperations.get(
                        matchId.toString(),
                        MatchAnalyticsDocument.class,
                        matchIndex());
        return Optional.ofNullable(document);
    }

    public List<MatchAnalyticsDocument> findMatchesByIds(Collection<UUID> matchIds) {
        if (matchIds.isEmpty()) {
            return List.of();
        }
        Criteria criteria = Criteria.where("matchId").in(matchIds);
        CriteriaQuery query = new CriteriaQuery(criteria);
        query.setMaxResults(matchIds.size());
        SearchHits<MatchAnalyticsDocument> hits =
                elasticsearchOperations.search(query, MatchAnalyticsDocument.class, matchIndex());
        return hits.getSearchHits().stream().map(SearchHit::getContent).toList();
    }

    public SearchHits<PlayerMatchDocument> searchPlayerMatches(
            PlayerMatchQuery query, int page, int size, Sort sort) {
        CriteriaQuery criteriaQuery = new CriteriaQuery(buildPlayerCriteria(query));
        criteriaQuery.setPageable(PageRequest.of(page, size, sort));
        criteriaQuery.setTrackTotalHits(true);
        return elasticsearchOperations.search(
                criteriaQuery, PlayerMatchDocument.class, playerMatchIndex());
    }

    public List<PlayerMatchDocument> scanPlayerMatches(PlayerMatchQuery query, int maxResults, Sort sort) {
        CriteriaQuery criteriaQuery = new CriteriaQuery(buildPlayerCriteria(query));
        criteriaQuery.setPageable(PageRequest.of(0, maxResults, sort));
        SearchHits<PlayerMatchDocument> hits =
                elasticsearchOperations.search(
                        criteriaQuery, PlayerMatchDocument.class, playerMatchIndex());
        return hits.getSearchHits().stream().map(SearchHit::getContent).toList();
    }

    public List<MatchAnalyticsDocument> findMatchesByTournamentKey(String tournamentKey, int maxResults) {
        Criteria criteria = Criteria.where("tournamentKey").is(tournamentKey);
        CriteriaQuery query = new CriteriaQuery(criteria);
        query.setPageable(PageRequest.of(0, maxResults));
        query.setTrackTotalHits(true);
        SearchHits<MatchAnalyticsDocument> hits =
                elasticsearchOperations.search(query, MatchAnalyticsDocument.class, matchIndex());
        return hits.getSearchHits().stream().map(SearchHit::getContent).toList();
    }

    public long countMatchesByTournamentKey(String tournamentKey) {
        Criteria criteria = Criteria.where("tournamentKey").is(tournamentKey);
        CriteriaQuery query = new CriteriaQuery(criteria);
        query.setMaxResults(0);
        query.setTrackTotalHits(true);
        SearchHits<MatchAnalyticsDocument> hits =
                elasticsearchOperations.search(query, MatchAnalyticsDocument.class, matchIndex());
        return hits.getTotalHits();
    }

    public Map<UUID, MatchAnalyticsDocument> mapMatchesById(Collection<UUID> matchIds) {
        Map<UUID, MatchAnalyticsDocument> mapped = new HashMap<>();
        for (MatchAnalyticsDocument match : findMatchesByIds(matchIds)) {
            mapped.put(match.getMatchId(), match);
        }
        return mapped;
    }

    public List<PlayerMatchDocument> exportPlayerMatches(PlayerMatchQuery query, int maxRows) {
        List<PlayerMatchDocument> rows = new ArrayList<>();
        int page = 0;
        int pageSize = AnalyticsQueryBounds.MAX_PAGE_SIZE;
        while (rows.size() < maxRows) {
            int remaining = maxRows - rows.size();
            int fetchSize = Math.min(pageSize, remaining);
            SearchHits<PlayerMatchDocument> hits =
                    searchPlayerMatches(query, page, fetchSize, RECENT_MATCH_SORT);
            if (hits.isEmpty()) {
                break;
            }
            hits.forEach(hit -> rows.add(hit.getContent()));
            if (hits.getSearchHits().size() < fetchSize) {
                break;
            }
            page++;
        }
        return rows;
    }

    public static Criteria buildPlayerCriteria(PlayerMatchQuery query) {
        Criteria criteria = Criteria.where("playerId").is(query.playerId().toString());
        if (query.opponentId() != null) {
            criteria = criteria.and(Criteria.where("opponentId").is(query.opponentId().toString()));
        }
        if (query.surface() != null && !query.surface().isBlank()) {
            criteria = criteria.and(Criteria.where("surface").is(query.surface()));
        }
        criteria = applyDateFrom(criteria, query.from());
        criteria = applyDateTo(criteria, query.to());
        return criteria;
    }

    private static Criteria applyDateFrom(Criteria criteria, Instant from) {
        if (from == null) {
            return criteria;
        }
        return criteria.subCriteria(
                new Criteria()
                        .or(Criteria.where("endedAt").greaterThanEqual(from))
                        .or(Criteria.where("scheduledAt").greaterThanEqual(from)));
    }

    private static Criteria applyDateTo(Criteria criteria, Instant to) {
        if (to == null) {
            return criteria;
        }
        return criteria.subCriteria(
                new Criteria()
                        .or(Criteria.where("endedAt").lessThanEqual(to))
                        .or(Criteria.where("scheduledAt").lessThanEqual(to)));
    }

    public static Sort recentMatchSort() {
        return RECENT_MATCH_SORT;
    }

    public static Sort chronologicalSort() {
        return CHRONOLOGICAL_SORT;
    }

    private IndexCoordinates matchIndex() {
        return IndexCoordinates.of(properties.matchAlias());
    }

    private IndexCoordinates playerMatchIndex() {
        return IndexCoordinates.of(properties.playerMatchAlias());
    }

    public record PlayerMatchQuery(
            UUID playerId, UUID opponentId, Instant from, Instant to, String surface) {}
}
