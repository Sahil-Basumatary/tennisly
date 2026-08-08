package dev.sahilbasumatary.analyticsservice.domain;

import java.util.Locale;
import java.util.regex.Pattern;

public final class TournamentKey {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern PUNCTUATION = Pattern.compile("[^a-z0-9]+");

    private TournamentKey() {}

    public static String from(String provider, String tournamentName, Integer season) {
        if (provider == null || provider.isBlank()) {
            throw new IllegalArgumentException("provider is required");
        }
        if (tournamentName == null || tournamentName.isBlank()) {
            throw new IllegalArgumentException("tournamentName is required");
        }
        if (season == null) {
            throw new IllegalArgumentException("season is required");
        }
        String normalized =
                PUNCTUATION
                        .matcher(
                                WHITESPACE
                                        .matcher(tournamentName.trim().toLowerCase(Locale.ROOT))
                                        .replaceAll(" "))
                        .replaceAll("-")
                        .replaceAll("^-+|-+$", "");
        return provider.trim().toLowerCase(Locale.ROOT) + "|" + normalized + "|" + season;
    }
}
