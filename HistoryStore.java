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
    private HistoryStore() {}

    public static synchronized void add(Context context, String source, String winner,
                                         String a, String b, String c) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            JSONArray old = new JSONArray(prefs.getString(KEY, "[]"));
            JSONArray updated = new JSONArray();
            JSONObject round = new JSONObject();
            round.put("time", new SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(new Date()));
            round.put("source", source);
            round.put("winner", winner);
            round.put("a", a);
            round.put("b", b);
            round.put("c", c);
            updated.put(round);
            for (int i = 0; i < old.length() && i < 29; i++) updated.put(old.getJSONObject(i));
            prefs.edit().putString(KEY, updated.toString()).apply();
        } catch (Exception ignored) { }
    }

    public static String summary(Context context) {
        try {
            JSONArray rounds = new JSONArray(context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, "[]"));
            if (rounds.length() == 0) return "No saved rounds yet.";
            int[] counts = new int[3];
            StringBuilder out = new StringBuilder("LATEST ROUNDS (MAX 30)\n");
            for (int i = 0; i < rounds.length(); i++) {
                JSONObject r = rounds.getJSONObject(i);
                String w = r.optString("winner");
                if ("A".equals(w)) counts[0]++;
                if ("B".equals(w)) counts[1]++;
                if ("C".equals(w)) counts[2]++;
                out.append(i + 1).append(". ").append(r.optString("time"))
                   .append(" • Winner: ").append(w).append("\n");
            }
            out.append("\nFrequency: A ").append(counts[0]).append(" • B ")
               .append(counts[1]).append(" • C ").append(counts[2]);
            out.append("\nFrequency describes saved rounds; it does not predict the next random result.");
            return out.toString();
        } catch (Exception e) {
            return "History unavailable.";
        }
    }
}
