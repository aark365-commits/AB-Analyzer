
package com.ab.analyzer;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {

    EditText playerA, playerB, playerC;
    TextView result;

    static class Hand {
        int type;
        int[] tie;
        String name;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(25, 35, 25, 25);
        layout.setBackgroundColor(Color.rgb(15, 18, 30));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(layout);

        TextView title = new TextView(this);
        title.setText("AB ANALYZER");
        title.setTextSize(30);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("TEEN PATTI CARD ANALYZER");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setGravity(Gravity.CENTER);
        layout.addView(subtitle);

        addSpace(layout);

        playerA = addPlayer(layout, "PLAYER A: AS KH 2D");
        playerB = addPlayer(layout, "PLAYER B: QS QH 5C");
        playerC = addPlayer(layout, "PLAYER C: JS JH 9D");

        Button analyze = new Button(this);
        analyze.setText("ANALYZE ALL 3 PLAYERS");
        layout.addView(analyze);

        result = new TextView(this);
        result.setTextColor(Color.WHITE);
        result.setTextSize(19);
        result.setGravity(Gravity.CENTER);
        result.setPadding(10, 30, 10, 20);
        layout.addView(result);

        analyze.setOnClickListener(v -> analyzeCards());

        setContentView(scroll);
    }

    private EditText addPlayer(LinearLayout layout, String hint) {
        TextView label = new TextView(this);
        label.setText(hint.startsWith("PLAYER A") ? "PLAYER A" :
                hint.startsWith("PLAYER B") ? "PLAYER B" : "PLAYER C");
        label.setTextColor(Color.WHITE);
        label.setTextSize(18);
        layout.addView(label);

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(hint);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        layout.addView(input);

        addSpace(layout);
        return input;
    }

    private void addSpace(LinearLayout layout) {
        TextView space = new TextView(this);
        space.setText("\n");
        layout.addView(space);
    }

    private int rank(String s) {
        switch (s) {
            case "A": return 14;
            case "K": return 13;
            case "Q": return 12;
            case "J": return 11;
            case "T": return 10;
            default:
                try {
                    int n = Integer.parseInt(s);
                    if (n >= 2 && n <= 9) return n;
                } catch (Exception e) {
                    return -1;
                }
                return -1;
        }
    }

    private Hand evaluate(String input, Set<String> allUsed) {

        String[] parts = input.trim().toUpperCase()
                .split("[,\\s]+");

        if (parts.length != 3)
            throw new IllegalArgumentException(
                    "Enter exactly 3 cards for each player.");

        int[] r = new int[3];
        char[] s = new char[3];

        for (int i = 0; i < 3; i++) {
            String card = parts[i];

            if (card.length() < 2 || card.length() > 3)
                throw new IllegalArgumentException("Invalid card: " + card);

            String value = card.substring(0, card.length() - 1);
            s[i] = card.charAt(card.length() - 1);

            if ("SHDC".indexOf(s[i]) < 0)
                throw new IllegalArgumentException("Invalid suit: " + card);

            r[i] = rank(value);

            if (r[i] < 0)
                throw new IllegalArgumentException("Invalid rank: " + card);

            if (!allUsed.add(card))
                throw new IllegalArgumentException("Duplicate card: " + card);
        }

        Arrays.sort(r);

        boolean flush = s[0] == s[1] && s[1] == s[2];
        boolean trail = r[0] == r[2];
        boolean pair = r[0] == r[1] || r[1] == r[2];

        boolean sequence =
                (r[0] + 1 == r[1] && r[1] + 1 == r[2])
                || (r[0] == 2 && r[1] == 3 && r[2] == 14);

        Hand h = new Hand();

        if (trail) {
            h.type = 6;
            h.name = "TRAIL";
            h.tie = new int[]{r[2]};
        }
        else if (flush && sequence) {
            h.type = 5;
            h.name = "PURE SEQUENCE";
            h.tie = sequenceHigh(r);
        }
        else if (sequence) {
            h.type = 4;
            h.name = "SEQUENCE";
            h.tie = sequenceHigh(r);
        }
        else if (flush) {
            h.type = 3;
            h.name = "COLOR";
            h.tie = new int[]{r[2], r[1], r[0]};
        }
        else if (pair) {
            h.type = 2;
            h.name = "PAIR";
            int p = r[1];
            int k = r[0] == r[1] ? r[2] : r[0];
            h.tie = new int[]{p, k};
        }
        else {
            h.type = 1;
            h.name = "HIGH CARD";
            h.tie = new int[]{r[2], r[1], r[0]};
        }

        return h;
    }

    private int[] sequenceHigh(int[] r) {
        if (r[0] == 2 && r[1] == 3 && r[2] == 14)
            return new int[]{13};

        return new int[]{r[2]};
    }

    private int compare(Hand a, Hand b) {
        if (a.type != b.type)
            return Integer.compare(a.type, b.type);

        for (int i = 0; i < Math.min(a.tie.length, b.tie.length); i++) {
            if (a.tie[i] != b.tie[i])
                return Integer.compare(a.tie[i], b.tie[i]);
        }

        return 0;
    }

    private void analyzeCards() {
        try {
            Set<String> used = new HashSet<>();

            Hand a = evaluate(playerA.getText().toString(), used);
            Hand b = evaluate(playerB.getText().toString(), used);
            Hand c = evaluate(playerC.getText().toString(), used);

            Hand[] hands = {a, b, c};
            String[] names = {"PLAYER A", "PLAYER B", "PLAYER C"};

            StringBuilder output = new StringBuilder();

            output.append("PLAYER A: ").append(a.name).append("\n");
            output.append("PLAYER B: ").append(b.name).append("\n");
            output.append("PLAYER C: ").append(c.name).append("\n\n");

            int best = 0;

            for (int i = 1; i < 3; i++) {
                if (compare(hands[i], hands[best]) > 0) {
                    best = i;
                }
            }

            output.append("WINNER: ");

            boolean first = true;

            for (int i = 0; i < 3; i++) {
                if (compare(hands[i], hands[best]) == 0) {
                    if (!first) output.append(" & ");
                    output.append(names[i]);
                    first = false;
                }
            }

            output.append("\n\nWinning Hand: ")
                    .append(hands[best].name);

            result.setText(output.toString());

        } catch (Exception e) {
            result.setText("ERROR: " + e.getMessage());
        }
    }
}
