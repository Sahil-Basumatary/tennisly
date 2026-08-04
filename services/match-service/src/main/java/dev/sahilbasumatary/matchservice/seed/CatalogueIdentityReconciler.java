package dev.sahilbasumatary.matchservice.seed;

import dev.sahilbasumatary.matchservice.entity.Match;
import dev.sahilbasumatary.matchservice.entity.MatchPlayer;
import dev.sahilbasumatary.matchservice.entity.MatchPoint;
import dev.sahilbasumatary.matchservice.entity.PlayerSide;
import dev.sahilbasumatary.matchservice.repository.MatchRepository;
import dev.sahilbasumatary.matchservice.seed.CataloguePlayerIdentity.MatchIdentity;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remaps catalogue match_player / point IDs onto tennis-data UUIDs when a cold seed used fallbacks.
 */
@Component
public class CatalogueIdentityReconciler {

    private static final Logger log = LoggerFactory.getLogger(CatalogueIdentityReconciler.class);

    private final MatchRepository matchRepository;
    private final CataloguePlayerResolver resolver;

    public CatalogueIdentityReconciler(
            MatchRepository matchRepository, CataloguePlayerResolver resolver) {
        this.matchRepository = matchRepository;
        this.resolver = resolver;
    }

    @Transactional
    public int reconcile() {
        int remapped = 0;
        for (MatchIdentity identity : CataloguePlayerIdentity.CATALOGUE_MATCHES) {
            Match match =
                    matchRepository
                            .findByExternalId(identity.matchExternalId())
                            .orElse(null);
            if (match == null) {
                continue;
            }
            UUID homeResolved = resolver.resolve(identity.homeExternalId());
            UUID awayResolved = resolver.resolve(identity.awayExternalId());
            MatchPlayer home = side(match, PlayerSide.HOME);
            MatchPlayer away = side(match, PlayerSide.AWAY);
            boolean changed = false;
            if (home != null && !home.getPlayerId().equals(homeResolved)) {
                remapPlayer(match, home.getPlayerId(), homeResolved);
                home.setPlayerId(homeResolved);
                changed = true;
            }
            if (away != null && !away.getPlayerId().equals(awayResolved)) {
                remapPlayer(match, away.getPlayerId(), awayResolved);
                away.setPlayerId(awayResolved);
                changed = true;
            }
            Map<String, Object> metadata = new HashMap<>(match.getMetadata());
            metadata.put("homeExternalId", identity.homeExternalId());
            metadata.put("awayExternalId", identity.awayExternalId());
            match.setMetadata(metadata);
            if (changed) {
                remapped++;
            }
            matchRepository.save(match);
        }
        if (remapped > 0) {
            log.info("Reconciled tennis-data player identities on {} catalogue matches", remapped);
        } else {
            log.info("Catalogue player identities already aligned with tennis-data (or fallback)");
        }
        return remapped;
    }

    private void remapPlayer(Match match, UUID from, UUID to) {
        for (MatchPoint point : match.getPoints()) {
            if (point.getServerId().equals(from)) {
                point.setServerId(to);
            }
            if (point.getWinnerId().equals(from)) {
                point.setWinnerId(to);
            }
            point.setScoreSnapshot(replaceId(point.getScoreSnapshot(), from, to));
        }
        match.setCurrentScore(replaceId(match.getCurrentScore(), from, to));
    }

    private static MatchPlayer side(Match match, PlayerSide side) {
        return match.getPlayers().stream()
                .filter(player -> player.getSide() == side)
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> replaceId(Map<String, Object> source, UUID from, UUID to) {
        if (source == null || source.isEmpty()) {
            return source;
        }
        String fromText = from.toString();
        String toText = to.toString();
        Map<String, Object> copy = new HashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String text && text.equals(fromText)) {
                copy.put(entry.getKey(), toText);
            } else if (value instanceof Map<?, ?> nested) {
                copy.put(entry.getKey(), replaceId((Map<String, Object>) nested, from, to));
            } else if (value instanceof java.util.List<?> list) {
                copy.put(
                        entry.getKey(),
                        list.stream()
                                .map(
                                        item -> {
                                            if (item instanceof Map<?, ?> nested) {
                                                return replaceId(
                                                        (Map<String, Object>) nested, from, to);
                                            }
                                            if (item instanceof String text && text.equals(fromText)) {
                                                return toText;
                                            }
                                            return item;
                                        })
                                .toList());
            } else {
                copy.put(entry.getKey(), value);
            }
        }
        return copy;
    }
}
