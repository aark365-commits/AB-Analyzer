package com.ab.analyzer;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.*;

public final class HistoryStore {
    private static final String PREF = "ab_history";
    private static final String KEY = "rounds";
    private static final int MAX_ROUNDS = 100;

    private HistoryStore() {}

    public static synchronized void add(Context context, String source, String winner,
                                         String a, String b, String c) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            JSONArray old = new JSONArray(prefs.getString(KEY, "[]"));
            JSONArray updated = new JSONArray();

            JSONObject round = new JSONObject();
            round.put("time", new SimpleDateFormat(
                    "dd MMM HH:mm", Locale.getDefault()).format(new Date()));
            round.put("source", source);
            round.put("winner", winner);
            round.put("a", a);
            round.put("b", b);
            round.put("c", c);

            updated.put(round);

            for (int i = 0; i < old.length() && i < MAX_ROUNDS - 1; i++) {
                updated.put(old.getJSONObject(i));
            }

            prefs.edit().putString(KEY, updated.toString()).apply();

        } catch (Exception ignored) {
        }
    }

    public static String summary(Context context) {
        try {
            JSONArray rounds = new JSONArray(
                    context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                            .getString(KEY, "[]"));

            if (rounds.length() == 0) {
                return "No saved rounds yet.";
            }

            int[] counts = new int[3];
            int ties = 0;

            for (int i = 0; i < rounds.length(); i++) {
                String w = rounds.getJSONObject(i).optString("winner");

                if ("A".equals(w)) counts[0]++;
                else if ("B".equals(w)) counts[1]++;
                else if ("C".equals(w)) counts[2]++;
                else if ("TIE".equals(w)) ties++;
            }

            int total = rounds.length();
            int decided = counts[0] + counts[1] + counts[2];

            StringBuilder out = new StringBuilder();
            out.append("HISTORY: ").append(total).append("/100 ROUNDS\n\n");
            out.append("WIN STATISTICS\n");

            out.append("Player A: ").append(counts[0])
                    .append(" wins (").append(percent(counts[0], decided)).append("%)\n");

            out.append("Player B: ").append(counts[1])
                    .append(" wins (").append(percent(counts[1], decided)).append("%)\n");

            out.append("Player C: ").append(counts[2])
                    .append(" wins (").append(percent(counts[2], decided)).append("%)\n");

            out.append("Ties: ").append(ties).append("\n");

            String leader = "No wins yet";
            int max = Math.max(counts[0], Math.max(counts[1], counts[2]));

            if (max > 0) {
                if (counts[0] == max && counts[1] == max && counts[2] == max) {
                    leader = "A, B and C (equal)";
                } else if (counts[0] == max && counts[1] == max) {
                    leader = "A and B (equal)";
                } else if (counts[0] == max && counts[2] == max) {
                    leader = "A and C (equal)";
                } else if (counts[1] == max && counts[2] == max) {
                    leader = "B and C (equal)";
                } else if (counts[0] == max) {
                    leader = "Player A";
                } else if (counts[1] == max) {
                    leader = "Player B";
                } else {
                    leader = "Player C";
                }
            }

            out.append("\nHISTORICAL LEADER: ").append(leader);
            out.append("\n\nLATEST ROUNDS\n");

            int show = Math.min(10, total);
            for (int i = 0; i < show; i++) {
                JSONObject r = rounds.getJSONObject(i);
                out.append(i + 1).append(". ")
                        .append(r.optString("time"))
                        .append(" • Winner: ")
                        .append(r.optString("winner"))
                        .append("\n");
            }

            out.append("\nNote: Past results do not reliably predict the next random round.");
            return out.toString();

        } catch (Exception e) {
            return "History unavailable.";
        }
    }

    private static int percent(int wins, int total) {
        if (total == 0) return 0;
        return Math.round((wins * 100f) / total);
    }
}
