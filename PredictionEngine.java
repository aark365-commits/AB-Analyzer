package com.ab.analyzer;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Descriptive, local-only analysis of confirmed historical rounds.
 * It does not inspect hidden cards or predict random future outcomes.
 */
public final class PredictionEngine {
    private PredictionEngine() {}

    public static String estimate(Context context, int window) {
        JSONArray rounds = HistoryStore.rounds(context);
        int n = Math.min(Math.max(window, 1), rounds.length());
        if (n == 0) {
            return "HISTORICAL ESTIMATE\n\nSave confirmed rounds first. No prediction is available yet.";
        }

        int[] wins = new int[3];
        int ties = 0;
        for (int i = 0; i < n; i++) {
            JSONObject round = rounds.optJSONObject(i);
            if (round == null) continue;
            String winner = round.optString("winner", "");
            if ("A".equals(winner) || "B".equals(winner) || "C".equals(winner)) {
                wins[winner.charAt(0) - 'A']++;
            } else if ("TIE".equals(winner)) {
                ties++;
            }
        }

        int decisive = wins[0] + wins[1] + wins[2];
        if (decisive == 0) {
            return "HISTORICAL ESTIMATE\n\nNo decisive confirmed rounds in the selected window.";
        }

        int max = Math.max(wins[0], Math.max(wins[1], wins[2]));
        StringBuilder leaders = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            if (wins[i] == max) {
                if (leaders.length() > 0) leaders.append(" / ");
                leaders.append((char)('A' + i));
            }
        }

        return "HISTORICAL FREQUENCY — LAST " + n + " ROUNDS\n\n"
                + "A: " + wins[0] + " wins • " + percent(wins[0], decisive) + "%\n"
                + "B: " + wins[1] + " wins • " + percent(wins[1], decisive) + "%\n"
                + "C: " + wins[2] + " wins • " + percent(wins[2], decisive) + "%\n"
                + "Ties: " + ties + "\n\n"
                + "Most frequent historical winner: " + leaders + "\n"
                + "Retrospective accuracy: " + retrospectiveAccuracy(rounds, n) + "\n\n"
                + "These are past frequencies, NOT future win probabilities. Random or hidden cards cannot be inferred.";
    }

    private static String retrospectiveAccuracy(JSONArray newestFirst, int window) {
        // Replay chronologically. For each round, use only earlier rounds to select
        // the most frequent winner, then compare that simple baseline with the result.
        int size = Math.min(window, newestFirst.length());
        int[] prior = new int[3];
        int tested = 0, correct = 0;
        for (int chronological = size - 1; chronological >= 0; chronological--) {
            JSONObject round = newestFirst.optJSONObject(chronological);
            if (round == null) continue;
            String actual = round.optString("winner", "");
            if (tested > 0 && ("A".equals(actual) || "B".equals(actual) || "C".equals(actual))) {
                int best = 0;
                for (int j = 1; j < 3; j++) if (prior[j] > prior[best]) best = j;
                if (prior[best] > 0 && actual.charAt(0) - 'A' == best) correct++;
            }
            if ("A".equals(actual) || "B".equals(actual) || "C".equals(actual)) {
                prior[actual.charAt(0) - 'A']++;
                tested++;
            }
        }
        if (tested < 2) return "not enough prior rounds";
        int comparisons = Math.max(0, tested - 1);
        return comparisons == 0 ? "not available" : correct + "/" + comparisons
                + " (" + percent(correct, comparisons) + "%; simple past-leader baseline)";
    }

    private static int percent(int value, int total) {
        return total == 0 ? 0 : Math.round(value * 100f / total);
    }
}
