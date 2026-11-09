package com.ab.analyzer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.text.InputType;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int CAPTURE_REQUEST = 1001;
    private static final String PREFS = "ab_analyzer_history";
    private final EditText[] cardInputs = new EditText[3];
    private TextView resultView, scanView, scanStatus, historyView;
    private MediaProjectionManager projectionManager;
    private boolean receiverRegistered = false;

    private final BroadcastReceiver ocrReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra(ScreenCaptureService.EXTRA_OCR_TEXT);
            scanView.setText(text == null || text.trim().isEmpty()
                    ? "No readable text detected in this frame." : text);
            scanStatus.setText("Screen capture is active • OCR preview updated");
        }
    };

    private static class Hand {
        final int category;
        final int[] tieBreak;
        final String name;
        final String cards;
        Hand(int category, int[] tieBreak, String name, String cards) {
            this.category = category;
            this.tieBreak = tieBreak;
            this.name = name;
            this.cards = cards;
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(16, 21, 34));
        getWindow().setNavigationBarColor(Color.rgb(16, 21, 34));

        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(24), dp(20), dp(30));
        page.setBackgroundColor(Color.rgb(16, 21, 34));
        scroll.addView(page);

        TextView title = label("AB ANALYZER", 29, Color.WHITE);
        title.setGravity(Gravity.CENTER);
        page.addView(title);
        TextView subtitle = label("TEEN PATTI • 3 PLAYER ANALYSIS", 13, 0xFF9EACC4);
        subtitle.setGravity(Gravity.CENTER);
        page.addView(subtitle);
        page.addView(space());
        page.addView(label("Enter revealed cards", 19, 0xFFFFC58A));
        page.addView(label("Example: AS KH 2D  |  S=Spades H=Hearts D=Diamonds C=Clubs", 13, 0xFFB8C3D6));

        String[] names = {"PLAYER A", "PLAYER B", "PLAYER C"};
        String[] hints = {"AS KH 2D", "QS QH 5C", "JS JH 9D"};
        for (int i = 0; i < 3; i++) {
            page.addView(label(names[i], 15, 0xFFFFC58A));
            cardInputs[i] = new EditText(this);
            cardInputs[i].setSingleLine(true);
            cardInputs[i].setTextSize(17);
            cardInputs[i].setTextColor(Color.WHITE);
            cardInputs[i].setHintTextColor(0xFF8290A8);
            cardInputs[i].setHint(hints[i]);
            cardInputs[i].setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
            page.addView(cardInputs[i]);
        }

        Button analyze = button("ANALYZE ALL 3 PLAYERS");
        page.addView(analyze);
        analyze.setOnClickListener(v -> analyzeHands());
        resultView = label("Enter three cards for each player, then tap Analyze.", 15, Color.WHITE);
        resultView.setPadding(6, dp(14), 6, dp(18));
        page.addView(resultView);

        page.addView(label("RECENT ROUND HISTORY", 18, 0xFFFFC58A));
        historyView = label("No saved rounds yet.", 13, 0xFFB8C3D6);
        page.addView(historyView);
        Button clearHistory = button("CLEAR HISTORY");
        page.addView(clearHistory);
        clearHistory.setOnClickListener(v -> {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().clear().apply();
            refreshHistory();
        });

        page.addView(space());
        page.addView(label("LIVE SCREEN OCR PREVIEW", 18, 0xFFFFC58A));
        scanStatus = label("Screen scanning is off. Android will ask permission before capture.", 13, 0xFFB8C3D6);
        page.addView(scanStatus);
        Button startScan = button("START SCREEN SCAN");
        page.addView(startScan);
        startScan.setOnClickListener(v -> requestCapture());
        Button stopScan = button("STOP SCREEN SCAN");
        page.addView(stopScan);
        stopScan.setOnClickListener(v -> {
            stopService(new Intent(this, ScreenCaptureService.class));
            scanStatus.setText("Screen scanning stopped.");
        });
        scanView = label("Recognized on-screen text will appear here. OCR preview does not yet map card images to Player A/B/C automatically.", 13, Color.WHITE);
        scanView.setPadding(6, dp(8), 6, dp(16));
        page.addView(scanView);
        page.addView(label("Privacy: screen capture starts only after your Android permission. Processing is on-device; this app does not connect to StreamKar private servers.", 12, 0xFF8290A8));

        setContentView(scroll);
        refreshHistory();

        IntentFilter filter = new IntentFilter(ScreenCaptureService.ACTION_OCR_RESULT);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(ocrReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(ocrReceiver, filter);
        }
        receiverRegistered = true;
    }

    private void requestCapture() {
        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        if (projectionManager == null) {
            scanStatus.setText("Screen capture is not available on this device.");
            return;
        }
        startActivityForResult(projectionManager.createScreenCaptureIntent(), CAPTURE_REQUEST);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != CAPTURE_REQUEST) return;
        if (resultCode == RESULT_OK && data != null) {
            Intent service = new Intent(this, ScreenCaptureService.class);
            service.putExtra("resultCode", resultCode);
            service.putExtra("data", data);
            try {
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(service);
                else startService(service);
                scanStatus.setText("Permission granted. OCR screen preview is starting.");
            } catch (Exception e) {
                scanStatus.setText("Could not start scanner: " + e.getMessage());
            }
        } else {
            scanStatus.setText("Screen capture permission was not granted.");
        }
    }

    private int rankValue(String rank) {
        switch (rank) {
            case "A": return 14;
            case "K": return 13;
            case "Q": return 12;
            case "J": return 11;
            default: return Integer.parseInt(rank);
        }
    }

    private Hand evaluate(String raw) {
        String[] cards = raw.trim().toUpperCase().split("[,\\s]+");
        if (cards.length != 3) throw new IllegalArgumentException("Har player ke exactly 3 cards likho.");
        int[] ranks = new int[3];
        String[] suits = new String[3];
        Set<String> unique = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            String card = cards[i];
            if (card.length() < 2 || card.length() > 3) throw new IllegalArgumentException("Invalid card: " + card);
            String suit = card.substring(card.length() - 1);
            String value = card.substring(0, card.length() - 1);
            if (!Arrays.asList("A", "K", "Q", "J", "10", "9", "8", "7", "6", "5", "4", "3", "2").contains(value)
                    || !Arrays.asList("S", "H", "D", "C").contains(suit)) {
                throw new IllegalArgumentException("Invalid card: " + card + " (example AS, 10H, 2D)");
            }
            if (!unique.add(card)) throw new IllegalArgumentException("Duplicate card: " + card);
            ranks[i] = rankValue(value);
            suits[i] = suit;
        }
        Arrays.sort(ranks);
        boolean trail = ranks[0] == ranks[2];
        boolean flush = suits[0].equals(suits[1]) && suits[1].equals(suits[2]);
        int sequence = 0;
        if (ranks[0] == 2 && ranks[1] == 3 && ranks[2] == 14) sequence = 15;
        else if (ranks[0] == 12 && ranks[1] == 13 && ranks[2] == 14) sequence = 14;
        else if (ranks[0] + 1 == ranks[1] && ranks[1] + 1 == ranks[2]) sequence = ranks[2];

        if (trail) return new Hand(6, new int[]{ranks[2]}, "TRAIL", raw);
        if (sequence > 0 && flush) return new Hand(5, new int[]{sequence}, "PURE SEQUENCE", raw);
        if (sequence > 0) return new Hand(4, new int[]{sequence}, "SEQUENCE", raw);
        if (flush) return new Hand(3, new int[]{ranks[2], ranks[1], ranks[0]}, "COLOR", raw);
        if (ranks[0] == ranks[1]) return new Hand(2, new int[]{ranks[0], ranks[2]}, "PAIR", raw);
        if (ranks[1] == ranks[2]) return new Hand(2, new int[]{ranks[1], ranks[0]}, "PAIR", raw);
        return new Hand(1, new int[]{ranks[2], ranks[1], ranks[0]}, "HIGH CARD", raw);
    }

    private int compare(Hand a, Hand b) {
        if (a.category != b.category) return Integer.compare(a.category, b.category);
        for (int i = 0; i < Math.min(a.tieBreak.length, b.tieBreak.length); i++) {
            if (a.tieBreak[i] != b.tieBreak[i]) return Integer.compare(a.tieBreak[i], b.tieBreak[i]);
        }
        return 0;
    }

    private void analyzeHands() {
        try {
            Hand[] hands = new Hand[3];
            String[] names = {"PLAYER A", "PLAYER B", "PLAYER C"};
            Set<String> allCards = new HashSet<>();
            for (int i = 0; i < 3; i++) {
                String raw = cardInputs[i].getText().toString().trim().toUpperCase();
                hands[i] = evaluate(raw);
                for (String card : raw.split("[,\\s]+")) {
                    if (!allCards.add(card)) throw new IllegalArgumentException("Same card multiple players mein use nahi ho sakta: " + card);
                }
            }
            int best = 0;
            for (int i = 1; i < 3; i++) if (compare(hands[i], hands[best]) > 0) best = i;
            ArrayList<String> winners = new ArrayList<>();
            for (int i = 0; i < 3; i++) if (compare(hands[i], hands[best]) == 0) winners.add(names[i]);

            StringBuilder out = new StringBuilder("AB ANALYZER RESULTS\n\n");
            for (int i = 0; i < 3; i++) {
                out.append(names[i]).append(" • ").append(hands[i].cards).append("\n")
                        .append("Hand: ").append(hands[i].name).append("\n\n");
            }
            out.append("RESULT: ").append(winners.size() == 1 ? "WINNER " : "TIE ")
                    .append(join(winners)).append("\nWinning hand: ").append(hands[best].name);
            resultView.setText(out.toString());
            saveRound(out.toString());
        } catch (Exception e) {
            resultView.setText("ERROR: " + e.getMessage());
        }
    }

    private String join(ArrayList<String> values) {
        StringBuilder b = new StringBuilder();
        for (String value : values) { if (b.length() > 0) b.append(" & "); b.append(value); }
        return b.toString();
    }

    private void saveRound(String summary) {
        android.content.SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        ArrayList<String> rows = new ArrayList<>();
        String old = prefs.getString("rounds", "");
        if (old != null && !old.isEmpty()) rows.addAll(Arrays.asList(old.split("\\n---\\n")));
        rows.add(0, DateFormat.getDateTimeInstance().format(new Date()) + "\n" + summary);
        while (rows.size() > 30) rows.remove(rows.size() - 1);
        StringBuilder joined = new StringBuilder();
        for (String row : rows) { if (joined.length() > 0) joined.append("\n---\n"); joined.append(row); }
        prefs.edit().putString("rounds", joined.toString()).apply();
        refreshHistory();
    }

    private void refreshHistory() {
        if (historyView == null) return;
        String saved = getSharedPreferences(PREFS, MODE_PRIVATE).getString("rounds", "");
        if (saved == null || saved.isEmpty()) historyView.setText("No saved rounds yet.");
        else historyView.setText(saved);
    }

    private TextView label(String text, int size, int color) {
        TextView view = new TextView(this);
        view.setText(text); view.setTextSize(size); view.setTextColor(color);
        view.setPadding(dp(4), dp(7), dp(4), dp(7));
        return view;
    }
    private View space() { return label(" ", 8, Color.WHITE); }
    private Button button(String text) {
        Button b = new Button(this); b.setText(text); b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFB52B69));
        return b;
    }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    @Override protected void onDestroy() {
        if (receiverRegistered) {
            try { unregisterReceiver(ocrReceiver); } catch (Exception ignored) { }
        }
        super.onDestroy();
    }
}
