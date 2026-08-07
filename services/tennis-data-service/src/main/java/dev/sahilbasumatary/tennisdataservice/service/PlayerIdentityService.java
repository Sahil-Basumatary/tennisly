package dev.sahilbasumatary.tennisdataservice.service;

import dev.sahilbasumatary.tennisdataservice.dto.UpstreamMatchData;
import dev.sahilbasumatary.tennisdataservice.dto.response.PlayerResponse;
import dev.sahilbasumatary.tennisdataservice.entity.Gender;
import dev.sahilbasumatary.tennisdataservice.entity.Player;
import dev.sahilbasumatary.tennisdataservice.entity.PlayerProviderRef;
import dev.sahilbasumatary.tennisdataservice.repository.PlayerProviderRefRepository;
import dev.sahilbasumatary.tennisdataservice.repository.PlayerRepository;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayerIdentityService {

    private static final Logger log = LoggerFactory.getLogger(PlayerIdentityService.class);

    public static final String PROVIDER_LTA = "livetennis";
    public static final String PROVIDER_BDL = "balldontlie";

    private final PlayerRepository playerRepository;
    private final PlayerProviderRefRepository providerRefRepository;

    public PlayerIdentityService(
            PlayerRepository playerRepository, PlayerProviderRefRepository providerRefRepository) {
        this.playerRepository = playerRepository;
        this.providerRefRepository = providerRefRepository;
    }

    @Transactional
    public PlayerResponse resolve(UpstreamMatchData.UpstreamPlayerRef ref, Gender genderHint) {
        if (ref == null) {
            throw new IllegalArgumentException("Player ref is required");
        }
        if (ref.providerPlayerId() != null) {
            Optional<PlayerProviderRef> existing =
                    providerRefRepository.findByProviderAndProviderRef(
                            PROVIDER_LTA, String.valueOf(ref.providerPlayerId()));
            if (existing.isPresent()) {
                return PlayerResponse.from(existing.get().getPlayer());
            }
        }

        Optional<Player> byName = matchByName(ref.firstName(), ref.lastName(), ref.displayName());
        Player player =
                byName.orElseGet(
                        () -> createFromLiveTennis(ref, genderHint == null ? Gender.MALE : genderHint));

        if (ref.providerPlayerId() != null) {
            link(player, PROVIDER_LTA, String.valueOf(ref.providerPlayerId()));
        }
        return PlayerResponse.from(player);
    }

    @Transactional
    public PlayerResponse resolve(
            Long providerPlayerId,
            String firstName,
            String lastName,
            String displayName,
            String gender) {
        Gender genderHint =
                gender != null && gender.equalsIgnoreCase("FEMALE") ? Gender.FEMALE : Gender.MALE;
        return resolve(
                new UpstreamMatchData.UpstreamPlayerRef(
                        providerPlayerId, firstName, lastName, displayName),
                genderHint);
    }

    private Optional<Player> matchByName(String firstName, String lastName, String displayName) {
        String targetLast = normalize(lastName);
        String targetFirst = normalize(firstName);
        if (targetLast.isBlank() && displayName != null) {
            String[] parts = displayName.trim().split("\\s+");
            targetLast = normalize(parts[parts.length - 1]);
            if (parts.length > 1 && targetFirst.isBlank()) {
                targetFirst = normalize(parts[0]);
            }
        }
        if (targetLast.isBlank()) {
            return Optional.empty();
        }
        List<Player> candidates = playerRepository.findByActiveTrueOrderByCurrentRankingAsc();
        String finalFirst = targetFirst;
        String finalLast = targetLast;
        List<Player> lastMatches =
                candidates.stream()
                        .filter(player -> normalize(player.getLastName()).equals(finalLast))
                        .toList();
        if (lastMatches.isEmpty()) {
            return Optional.empty();
        }
        if (!finalFirst.isBlank()) {
            Optional<Player> exact =
                    lastMatches.stream()
                            .filter(player -> normalize(player.getFirstName()).equals(finalFirst))
                            .findFirst();
            if (exact.isPresent()) {
                return exact;
            }
            Optional<Player> initial =
                    lastMatches.stream()
                            .filter(
                                    player ->
                                            !normalize(player.getFirstName()).isEmpty()
                                                    && normalize(player.getFirstName()).charAt(0)
                                                            == finalFirst.charAt(0))
                            .findFirst();
            if (initial.isPresent()) {
                return initial;
            }
        }
        return lastMatches.size() == 1 ? Optional.of(lastMatches.get(0)) : Optional.empty();
    }

    private Player createFromLiveTennis(
            UpstreamMatchData.UpstreamPlayerRef ref, Gender gender) {
        Player player = new Player();
        String externalId =
                ref.providerPlayerId() == null
                        ? "lta-name-" + normalize(ref.displayName()).replace(' ', '-')
                        : "lta-" + ref.providerPlayerId();
        player.setExternalId(externalId);
        player.setFirstName(blankToUnknown(ref.firstName()));
        player.setLastName(blankToUnknown(ref.lastName().isBlank() ? ref.displayName() : ref.lastName()));
        player.setGender(gender);
        player.setActive(true);
        return playerRepository.save(player);
    }

    private void link(Player player, String provider, String providerRef) {
        if (providerRefRepository.findByProviderAndProviderRef(provider, providerRef).isPresent()) {
            return;
        }
        // One provider mapping per player — a second LTA id name-matching the same BallDontLie
        // row must not invent a conflicting link.
        Optional<PlayerProviderRef> byPlayer =
                providerRefRepository.findByPlayerIdAndProvider(player.getId(), provider);
        if (byPlayer.isPresent()) {
            String existingRef = byPlayer.get().getProviderRef();
            if (!existingRef.equals(providerRef)) {
                log.warn(
                        "Refusing to overwrite {} link for playerId={} existingRef={} newRef={}",
                        provider,
                        player.getId(),
                        existingRef,
                        providerRef);
            }
            return;
        }
        providerRefRepository.insertIgnoreConflict(player.getId(), provider, providerRef);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String stripped =
                Normalizer.normalize(value, Normalizer.Form.NFD)
                        .replaceAll("\\p{M}+", "")
                        .toLowerCase(Locale.ROOT)
                        .replaceAll("[^a-z]", "");
        return stripped;
    }

    private static String blankToUnknown(String value) {
        return value == null || value.isBlank() ? "Unknown" : value.trim();
    }
}
