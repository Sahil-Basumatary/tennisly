package dev.sahilbasumatary.matchservice.seed;

import java.util.UUID;

/**
 * Stable catalogue identifiers so web routes, docs, and replay curls stay pinned across rebuilds.
 * Tournament UUIDs are synthetic stand-ins until tennis-data tournament rows are wired through.
 */
public final class BroadcastCatalogueIds {

    public static final UUID TOURNAMENT_WIMBLEDON =
            UUID.fromString("a1111111-1111-4111-8111-111111111111");
    public static final UUID TOURNAMENT_ROLAND_GARROS =
            UUID.fromString("a2222222-2222-4222-8222-222222222222");
    public static final UUID TOURNAMENT_US_OPEN =
            UUID.fromString("a3333333-3333-4333-8333-333333333333");

    public static final UUID PLAYER_ALCARAZ =
            UUID.fromString("b1000000-0000-4000-8000-000000000001");
    public static final UUID PLAYER_SINNER =
            UUID.fromString("b1000000-0000-4000-8000-000000000002");
    public static final UUID PLAYER_DJOKOVIC =
            UUID.fromString("b1000000-0000-4000-8000-000000000003");
    public static final UUID PLAYER_MEDVEDEV =
            UUID.fromString("b1000000-0000-4000-8000-000000000004");
    public static final UUID PLAYER_ZVEREV =
            UUID.fromString("b1000000-0000-4000-8000-000000000005");
    public static final UUID PLAYER_RUUD =
            UUID.fromString("b1000000-0000-4000-8000-000000000006");
    public static final UUID PLAYER_SWIATEK =
            UUID.fromString("b1000000-0000-4000-8000-000000000007");
    public static final UUID PLAYER_GAUFF =
            UUID.fromString("b1000000-0000-4000-8000-000000000008");
    public static final UUID PLAYER_SABALENKA =
            UUID.fromString("b1000000-0000-4000-8000-000000000009");
    public static final UUID PLAYER_RYBAKINA =
            UUID.fromString("b1000000-0000-4000-8000-00000000000a");
    public static final UUID PLAYER_KEYS =
            UUID.fromString("b1000000-0000-4000-8000-00000000000b");
    public static final UUID PLAYER_FONSECA =
            UUID.fromString("b1000000-0000-4000-8000-00000000000c");
    public static final UUID PLAYER_RUBLEV =
            UUID.fromString("b1000000-0000-4000-8000-00000000000d");
    public static final UUID PLAYER_PAOLINI =
            UUID.fromString("b1000000-0000-4000-8000-00000000000e");
    public static final UUID PLAYER_TSITSIPAS =
            UUID.fromString("b1000000-0000-4000-8000-00000000000f");

    public static final UUID MATCH_WIM_SF_ALC_SIN =
            UUID.fromString("c2000000-0000-4000-8000-000000000001");
    public static final UUID MATCH_WIM_SF_DJO_MED =
            UUID.fromString("c2000000-0000-4000-8000-000000000002");
    public static final UUID MATCH_WIM_QF_ZVE_RUU =
            UUID.fromString("c2000000-0000-4000-8000-000000000003");
    public static final UUID MATCH_WIM_QF_SWI_GAU =
            UUID.fromString("c2000000-0000-4000-8000-000000000004");
    public static final UUID MATCH_WIM_QF_SAB_RYB =
            UUID.fromString("c2000000-0000-4000-8000-000000000005");
    public static final UUID MATCH_RG_QF_ALC_ZVE =
            UUID.fromString("c2000000-0000-4000-8000-000000000006");
    public static final UUID MATCH_RG_SF_SIN_DJO =
            UUID.fromString("c2000000-0000-4000-8000-000000000007");
    public static final UUID MATCH_RG_R16_GAU_KEY =
            UUID.fromString("c2000000-0000-4000-8000-000000000008");
    public static final UUID MATCH_USO_R32_SIN_FON =
            UUID.fromString("c2000000-0000-4000-8000-000000000009");
    public static final UUID MATCH_USO_R32_MED_RUB =
            UUID.fromString("c2000000-0000-4000-8000-00000000000a");
    public static final UUID MATCH_USO_R16_SWI_PAO =
            UUID.fromString("c2000000-0000-4000-8000-00000000000b");
    public static final UUID MATCH_USO_R64_RUU_TSI =
            UUID.fromString("c2000000-0000-4000-8000-00000000000c");

    private BroadcastCatalogueIds() {}
}
