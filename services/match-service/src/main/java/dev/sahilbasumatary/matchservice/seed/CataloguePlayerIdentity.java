package dev.sahilbasumatary.matchservice.seed;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shared street-name identity ({@code externalId}) for catalogue players. Tennis-data owns the
 * durable UUID; these fallback UUIDs keep seeding/replay working when that service is offline.
 */
public final class CataloguePlayerIdentity {

    public static final String EXT_SINNER = "atp-001";
    public static final String EXT_ZVEREV = "atp-002";
    public static final String EXT_ALCARAZ = "atp-003";
    public static final String EXT_MEDVEDEV = "atp-005";
    public static final String EXT_RUUD = "atp-006";
    public static final String EXT_DJOKOVIC = "atp-007";
    public static final String EXT_RUBLEV = "atp-009";
    public static final String EXT_TSITSIPAS = "atp-014";
    public static final String EXT_FONSECA = "atp-031";
    public static final String EXT_SABALENKA = "wta-001";
    public static final String EXT_SWIATEK = "wta-002";
    public static final String EXT_GAUFF = "wta-003";
    public static final String EXT_PAOLINI = "wta-004";
    public static final String EXT_RYBAKINA = "wta-006";
    public static final String EXT_KEYS = "wta-013";

    public static final Map<String, UUID> FALLBACK_BY_EXTERNAL =
            Map.ofEntries(
                    Map.entry(EXT_ALCARAZ, BroadcastCatalogueIds.PLAYER_ALCARAZ),
                    Map.entry(EXT_SINNER, BroadcastCatalogueIds.PLAYER_SINNER),
                    Map.entry(EXT_DJOKOVIC, BroadcastCatalogueIds.PLAYER_DJOKOVIC),
                    Map.entry(EXT_MEDVEDEV, BroadcastCatalogueIds.PLAYER_MEDVEDEV),
                    Map.entry(EXT_ZVEREV, BroadcastCatalogueIds.PLAYER_ZVEREV),
                    Map.entry(EXT_RUUD, BroadcastCatalogueIds.PLAYER_RUUD),
                    Map.entry(EXT_SWIATEK, BroadcastCatalogueIds.PLAYER_SWIATEK),
                    Map.entry(EXT_GAUFF, BroadcastCatalogueIds.PLAYER_GAUFF),
                    Map.entry(EXT_SABALENKA, BroadcastCatalogueIds.PLAYER_SABALENKA),
                    Map.entry(EXT_RYBAKINA, BroadcastCatalogueIds.PLAYER_RYBAKINA),
                    Map.entry(EXT_KEYS, BroadcastCatalogueIds.PLAYER_KEYS),
                    Map.entry(EXT_FONSECA, BroadcastCatalogueIds.PLAYER_FONSECA),
                    Map.entry(EXT_RUBLEV, BroadcastCatalogueIds.PLAYER_RUBLEV),
                    Map.entry(EXT_PAOLINI, BroadcastCatalogueIds.PLAYER_PAOLINI),
                    Map.entry(EXT_TSITSIPAS, BroadcastCatalogueIds.PLAYER_TSITSIPAS));

    public record MatchIdentity(String matchExternalId, String homeExternalId, String awayExternalId) {}

    public static final List<MatchIdentity> CATALOGUE_MATCHES =
            List.of(
                    new MatchIdentity(
                            "wimbledon-2026-ms-sf-alcaraz-sinner", EXT_ALCARAZ, EXT_SINNER),
                    new MatchIdentity(
                            "wimbledon-2026-ms-sf-djokovic-medvedev", EXT_DJOKOVIC, EXT_MEDVEDEV),
                    new MatchIdentity(
                            "wimbledon-2026-ms-qf-zverev-ruud", EXT_ZVEREV, EXT_RUUD),
                    new MatchIdentity(
                            "wimbledon-2026-ws-qf-swiatek-gauff", EXT_SWIATEK, EXT_GAUFF),
                    new MatchIdentity(
                            "wimbledon-2026-ws-qf-sabalenka-rybakina", EXT_SABALENKA, EXT_RYBAKINA),
                    new MatchIdentity("rg-2026-ms-qf-alcaraz-zverev", EXT_ALCARAZ, EXT_ZVEREV),
                    new MatchIdentity("rg-2026-ms-sf-sinner-djokovic", EXT_SINNER, EXT_DJOKOVIC),
                    new MatchIdentity("rg-2026-ws-r16-gauff-keys", EXT_GAUFF, EXT_KEYS),
                    new MatchIdentity("uso-2026-ms-r32-sinner-fonseca", EXT_SINNER, EXT_FONSECA),
                    new MatchIdentity("uso-2026-ms-r32-medvedev-rublev", EXT_MEDVEDEV, EXT_RUBLEV),
                    new MatchIdentity("uso-2026-ws-r16-swiatek-paolini", EXT_SWIATEK, EXT_PAOLINI),
                    new MatchIdentity("uso-2026-ms-r64-ruud-tsitsipas", EXT_RUUD, EXT_TSITSIPAS));

    private CataloguePlayerIdentity() {}
}
