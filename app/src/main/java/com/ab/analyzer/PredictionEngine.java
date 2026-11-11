package com.ab.analyzer;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

/** Historical trend summary. This does not predict hidden cards or guarantee future results. */
public final class PredictionEngine {
    private PredictionEngine() {}

    public static String estimate(Context context, int window) {
        JSONArray rounds = HistoryStore.rounds(context);
        int limit = Math.max(0, Math.min(rounds.length(), window));
        int[] wins = new int[3];
        int ties = 0;
        int valid = 0;

        for (int i = 0; i < limit; i++) {
            JSONObject round = rounds.optJSONObject(i);
            if (round == null) continue;
            String winner = round.optString("winner", "").trim().toUpperCase();
            if ("A".equals(winner) || "B".equals(winner) || "C".equals(winner)) {
                wins[winner.charAt(0) - 'A']++;
                valid++;
            } else if ("TIE".equals(winner)) {
                ties++;
            }
        }

        if (valid == 0) {
            return "NOT ENOUGH DATA\n\nSave confirmed rounds with their actual winners to see historical frequencies.\n\nThese figures describe past results only; they are not guaranteed future probabilities.";
        }

        int leader = 0;
        for (int i = 1; i < 3; i++) if (wins[i] > wins[leader]) leader = i;
        boolean tiedLeader = (wins[0] == wins[1] && wins[1] >= wins[2])
                || (wins[0] == wins[2] && wins[2] >= wins[1])
                || (wins[1] == wins[2] && wins[2] >= wins[0]);

        StringBuilder out = new StringBuilder();
        out.append("HISTORICAL TREND — LAST ").append(limit).append(" ROUNDS\n\n");
        for (int i = 0; i < 3; i++) {
            int percent = Math.round(wins[i] * 100f / valid);
            out.append((char)('A' + i)).append(": ").append(wins[i]).append(" wins • ")
                    .append(percent).append("% of decisive saved rounds\n");
        }
        out.append("Ties: ").append(ties).append("\n\n");
        out.append("Historical leader: ");
        if (tiedLeader) {
            int max = Math.max(wins[0], Math.max(wins[1], wins[2]));
            boolean first = true;
            for (int i = 0; i < 3; i++) if (wins[i] == max) {
                if (!first) out.append(" / ");
                out.append((char)('A' + i));
                first = false;
            }
        } else {
            out.append((char)('A' + leader));
        }
        out.append("\n\nThis is a historical frequency, NOT a prediction of hidden cards or a reliable chance of winning the next round.");
        return out.toString();
    }
}
