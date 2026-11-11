
package com.ab.analyzer;
import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public class PredictionEngine {

    private static final String[] PLAYERS = {"A", "B", "C"};

    private static final Map<String, Integer> wins = new LinkedHashMap<>();
    private static final Map<String, Integer> rounds = new LinkedHashMap<>();

    static {
        for (String p : PLAYERS) {
            wins.put(p, 0);
            rounds.put(p, 0);
        }
    }

    public static class HandResult {
        public int rank;
        public int[] values;
        public String type;

        HandResult(int rank, int[] values, String type) {
            this.rank = rank;
            this.values = values;
            this.type = type;
        }
    }

    public static HandResult analyze(String[] cards) {
        if (cards == null || cards.length != 3)
            throw new IllegalArgumentException("Exactly 3 cards required");

        int[] ranks = new int[3];
        String[] suits = new String[3];

        for (int i = 0; i < 3; i++) {
            String card = cards[i].trim().toUpperCase();
            String suit = card.substring(card.length() - 1);
            String value = card.substring(0, card.length() - 1);

            if (!Arrays.asList("S", "H", "D", "C").contains(suit))
                throw new IllegalArgumentException("Invalid suit: " + card);

            int r;
            switch (value) {
                case "A": r = 14; break;
                case "K": r = 13; break;
                case "Q": r = 12; break;
                case "J": r = 11; break;
                default:
                    r = Integer.parseInt(value);
                    if (r < 2 || r > 10)
                        throw new IllegalArgumentException("Invalid card: " + card);
            }

            ranks[i] = r;
            suits[i] = suit;
        }

        Arrays.sort(ranks);

        boolean trail = ranks[0] == ranks[2];
        boolean color = suits[0].equals(suits[1])
                && suits[1].equals(suits[2]);

        boolean pair = ranks[0] == ranks[1]
                || ranks[1] == ranks[2];

        boolean sequence = ranks[1] == ranks[0] + 1
                && ranks[2] == ranks[1] + 1;

        boolean aceLow = ranks[0] == 2
                && ranks[1] == 3
                && ranks[2] == 14;

        if (aceLow) ranks = new int[]{1, 2, 3};

        if (trail)
            return new HandResult(6, ranks, "TRAIL");

        if (sequence && color || aceLow && color)
            return new HandResult(5, ranks, "PURE SEQUENCE");

        if (sequence || aceLow)
            return new HandResult(4, ranks, "SEQUENCE");

        if (color)
            return new HandResult(3, ranks, "COLOR");

        if (pair)
            return new HandResult(2, ranks, "PAIR");

        return new HandResult(1, ranks, "HIGH CARD");
    }

    public static int compare(HandResult a, HandResult b) {
        if (a.rank != b.rank)
            return Integer.compare(a.rank, b.rank);

        for (int i = 2; i >= 0; i--) {
            if (a.values[i] != b.values[i])
                return Integer.compare(a.values[i], b.values[i]);
        }

        return 0;
    }

    public static String predictWinner(
            String[] playerA,
            String[] playerB,
            String[] playerC) {

        HandResult a = analyze(playerA);
        HandResult b = analyze(playerB);
        HandResult c = analyze(playerC);

        HandResult[] hands = {a, b, c};

        int best = 0;
        boolean tie = false;

        for (int i = 1; i < 3; i++) {
            int result = compare(hands[i], hands[best]);

            if (result > 0) {
                best = i;
                tie = false;
            } else if (result == 0) {
                tie = true;
            }
        }

        if (tie) return "TIE";

        return PLAYERS[best];
    }

    public static void recordRound(String winner) {
        if (!wins.containsKey(winner))
            return;

        wins.put(winner, wins.get(winner) + 1);

        for (String p : PLAYERS)
            rounds.put(p, rounds.get(p) + 1);
    }

    public static String getStatistics() {
        StringBuilder result = new StringBuilder();

        for (String p : PLAYERS) {
            int total = rounds.get(p);
            int won = wins.get(p);

            double rate = total == 0 ? 0 :
                    (won * 100.0 / total);

            result.append("Player ").append(p)
                    .append(": ")
                    .append(won)
                    .append(" wins | ")
                    .append(String.format(Locale.US, "%.1f", rate))
                    .append("%\n");
        }

        return result.toString();
    }
}

public static String estimate(Context context, int window) {

    JSONArray rounds = HistoryStore.rounds(context);

    int total = Math.min(rounds.length(), window);

    if (total == 0) {
        return "NOT ENOUGH DATA\n\nSave confirmed rounds to generate historical analysis.";
    }

    int a = 0;
    int b = 0;
    int c = 0;
    int ties = 0;

    for (int i = 0; i < total; i++) {

        JSONObject round = rounds.optJSONObject(i);

        if (round == null) continue;

        String winner = round.optString("winner", "");

        switch (winner) {
            case "A":
                a++;
                break;

            case "B":
                b++;
                break;

            case "C":
                c++;
                break;

            case "TIE":
                ties++;
                break;
        }
    }

    int valid = a + b + c;

    if (valid == 0) {
        return "No valid historical winners found.";
    }

    double pa = a * 100.0 / valid;
    double pb = b * 100.0 / valid;
    double pc = c * 100.0 / valid;

    String leader = "TIE";

    int max = Math.max(a, Math.max(b, c));

    if (max == a && a > b && a > c) leader = "PLAYER A";
    else if (max == b && b > a && b > c) leader = "PLAYER B";
    else if (max == c && c > a && c > b) leader = "PLAYER C";

    return String.format(Locale.US,
        "SMART HISTORICAL ANALYSIS\n\n" +
        "Rounds analyzed: %d\n\n" +
        "Player A: %.1f%% (%d wins)\n" +
        "Player B: %.1f%% (%d wins)\n" +
        "Player C: %.1f%% (%d wins)\n\n" +
        "Ties: %d\n\n" +
        "Historical Leader: %s\n\n" +
        "These are past-result frequencies, not guaranteed next-round predictions.",
        valid, pa, a, pb, b, pc, c, ties, leader
    );
}


