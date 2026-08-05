package dev.sahilbasumatary.tennisdataservice.livetennis;

import dev.sahilbasumatary.tennisdataservice.dto.UpstreamPointData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Diffs consecutive Live Tennis score rows into point winners. Outcome and rally length are not in
 * the tape, so those stay UNKNOWN / null for honest downstream stats.
 */
final class PointTapeDecoder {

    private PointTapeDecoder() {}

    static List<UpstreamPointData> decode(List<LiveTennisModels.ScorePayload> tape) {
        if (tape == null || tape.size() < 2) {
            return List.of();
        }
        List<UpstreamPointData> points = new ArrayList<>();
        int sequence = 0;
        for (int i = 1; i < tape.size(); i++) {
            LiveTennisModels.ScorePayload prev = tape.get(i - 1);
            LiveTennisModels.ScorePayload curr = tape.get(i);
            Integer winnerSide = winnerSide(prev, curr);
            if (winnerSide == null) {
                continue;
            }
            sequence += 1;
            int serverSide = prev.server() == null || prev.server() < 1 || prev.server() > 2 ? 1 : prev.server();
            points.add(
                    new UpstreamPointData(
                            sequence,
                            serverSide,
                            winnerSide,
                            "UNKNOWN",
                            null,
                            snapshot(curr)));
        }
        return points;
    }

    private static Integer winnerSide(
            LiveTennisModels.ScorePayload prev, LiveTennisModels.ScorePayload curr) {
        int setProgress = compareSets(prev.sets(), curr.sets());
        if (setProgress != 0) {
            return setProgress;
        }
        int gameProgress = compareGames(prev.games(), curr.games());
        if (gameProgress != 0) {
            return gameProgress;
        }
        return comparePoints(prev.points(), curr.points());
    }

    private static int compareSets(List<Integer> before, List<Integer> after) {
        int b1 = sum(before, 0);
        int b2 = sum(before, 1);
        int a1 = sum(after, 0);
        int a2 = sum(after, 1);
        if (a1 > b1) return 1;
        if (a2 > b2) return 2;
        return 0;
    }

    private static int compareGames(List<List<Integer>> before, List<List<Integer>> after) {
        int b1 = lastOrZero(before, 0);
        int b2 = lastOrZero(before, 1);
        int a1 = lastOrZero(after, 0);
        int a2 = lastOrZero(after, 1);
        if (a1 > b1) return 1;
        if (a2 > b2) return 2;
        // New set started — previous set games may reset; treat as no in-game winner here.
        if (size(before) < size(after)) {
            return 0;
        }
        return 0;
    }

    private static Integer comparePoints(List<String> before, List<String> after) {
        int b1 = pointValue(at(before, 0));
        int b2 = pointValue(at(before, 1));
        int a1 = pointValue(at(after, 0));
        int a2 = pointValue(at(after, 1));
        if (a1 > b1) return 1;
        if (a2 > b2) return 2;
        // Game rolled over to 0-0 after a won game — already handled via games diff.
        return null;
    }

    private static Map<String, Object> snapshot(LiveTennisModels.ScorePayload score) {
        Map<String, Object> snap = new HashMap<>();
        snap.put("sets", score.sets() == null ? List.of() : score.sets());
        snap.put("games", score.games() == null ? List.of() : score.games());
        snap.put("points", score.points() == null ? List.of() : score.points());
        snap.put("server", score.server());
        snap.put("tiebreak", Boolean.TRUE.equals(score.tiebreak()));
        snap.put("timestamp", score.timestamp());
        return snap;
    }

    private static int sum(List<Integer> values, int index) {
        if (values == null || values.size() <= index || values.get(index) == null) {
            return 0;
        }
        return values.get(index);
    }

    private static int lastOrZero(List<List<Integer>> games, int sideIndex) {
        if (games == null || games.isEmpty()) {
            return 0;
        }
        List<Integer> last = games.get(games.size() - 1);
        if (last == null || last.size() <= sideIndex || last.get(sideIndex) == null) {
            return 0;
        }
        return last.get(sideIndex);
    }

    private static int size(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private static String at(List<String> points, int index) {
        if (points == null || points.size() <= index) {
            return "0";
        }
        return points.get(index);
    }

    private static int pointValue(String raw) {
        if (raw == null) {
            return 0;
        }
        return switch (raw.trim().toUpperCase()) {
            case "0", "LOVE" -> 0;
            case "15" -> 1;
            case "30" -> 2;
            case "40" -> 3;
            case "AD", "A", "ADV" -> 4;
            default -> {
                try {
                    yield Integer.parseInt(raw.trim());
                } catch (NumberFormatException ex) {
                    yield 0;
                }
            }
        };
    }
}
