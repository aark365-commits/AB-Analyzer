
package com.ab.analyzer;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {

    EditText playerA, playerB;
    TextView result;

    static class Card {
        int rank;
        char suit;
        Card(int r, char s) {
            rank = r;
            suit = s;
        }
    }

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

        playerA = new EditText(this);
        playerA.setSingleLine(true);
        playerA.setHint("Player A: AS KH 2D");
        playerA.setTextColor(Color.WHITE);
        playerA.setHintTextColor(Color.GRAY);
        layout.addView(playerA);

        playerB = new EditText(this);
        playerB.setSingleLine(true);
        playerB.setHint("Player B: QS QH 5C");
        playerB.setTextColor(Color.WHITE);
        playerB.setHintTextColor(Color.GRAY);
        layout.addView(playerB);

        Button analyze = new Button(this);
        analyze.setText("ANALYZE CARDS");
        layout.addView(analyze);

        result = new TextView(this);
        result.setTextColor(Color.WHITE);
        result.setTextSize(19);
        result.setGravity(Gravity.CENTER);
        result.setPadding(5, 30, 5, 20);
        layout.addView(result);

        analyze.setOnClickListener(v -> analyzeCards());

        setContentView(layout);
    }

    Card parseCard(String token) {
        token = token.toUpperCase().trim();

        if (token.length() < 2) return null;

        char suit = token.charAt(token.length() - 1);
        String rankText = token.substring(0, token.length() - 1);

        if ("SHDC".indexOf(suit) < 0) return null;

        int rank;

        switch (rankText) {
            case "A": rank = 14; break;
            case "K": rank = 13; break;
            case "Q": rank = 12; break;
            case "J": rank = 11; break;
            default:
                try {
                    rank = Integer.parseInt(rankText);
                } catch (Exception e) {
                    return null;
                }
        }

        if (rank < 2 || rank > 14) return null;

        return new Card(rank, suit);
    }

    Card[] parseHand(String input) {
        String[] parts = input.trim().toUpperCase()
                .split("[,\\s]+");

        if (parts.length != 3) return null;

        Card[] cards = new Card[3];

        for (int i = 0; i < 3; i++) {
            cards[i] = parseCard(parts[i]);
            if (cards[i] == null) return null;
        }

        return cards;
    }

    Hand evaluate(Card[] cards) {
        int[] r = {
            cards[0].rank,
            cards[1].rank,
            cards[2].rank
        };

        Arrays.sort(r);

        boolean flush =
            cards[0].suit == cards[1].suit &&
            cards[1].suit == cards[2].suit;

        boolean trail = r[0] == r[2];
        boolean pair = r[0] == r[1] || r[1] == r[2];

        boolean sequence =
            (r[1] == r[0] + 1 && r[2] == r[1] + 1)
            || (r[0] == 2 && r[1] == 3 && r[2] == 14);

        Hand h = new Hand();

        if (trail) {
            h.type = 6;
            h.name = "TRAIL";
            h.tie = new int[]{r[0]};
        }
        else if (sequence && flush) {
            h.type = 5;
            h.name = "PURE SEQUENCE";
            h.tie = new int[]{
                r[2] == 14 && r[1] == 3 ? 3 : r[2]
            };
        }
        else if (sequence) {
            h.type = 4;
            h.name = "SEQUENCE";
            h.tie = new int[]{
                r[2] == 14 && r[1] == 3 ? 3 : r[2]
            };
        }
        else if (flush) {
            h.type = 3;
            h.name = "COLOR";
            h.tie = new int[]{r[2], r[1], r[0]};
        }
        else if (pair) {
            int p = r[1];
            int kicker = r[0];

            if (r[0] == r[1]) {
                p = r[0];
                kicker = r[2];
            }

            h.type = 2;
            h.name = "PAIR";
            h.tie = new int[]{p, kicker};
        }
        else {
            h.type = 1;
            h.name = "HIGH CARD";
            h.tie = new int[]{r[2], r[1], r[0]};
        }

        return h;
    }

    int compare(Hand a, Hand b) {
        if (a.type != b.type)
            return Integer.compare(a.type, b.type);

        for (int i = 0; i < a.tie.length; i++) {
            if (a.tie[i] != b.tie[i])
                return Integer.compare(a.tie[i], b.tie[i]);
        }

        return 0;
    }

    void analyzeCards() {
        Card[] a = parseHand(playerA.getText().toString());
        Card[] b = parseHand(playerB.getText().toString());

        if (a == null || b == null) {
            result.setText("ERROR!\nEnter 3 valid cards for each player.");
            return;
        }

        HashSet<String> used = new HashSet<>();

        for (Card c : a) {
            used.add(c.rank + "" + c.suit);
        }

        for (Card c : b) {
            String key = c.rank + "" + c.suit;

            if (!used.add(key)) {
                result.setText("ERROR!\nDuplicate card detected!");
                return;
            }
        }

        Hand handA = evaluate(a);
        Hand handB = evaluate(b);

        int winner = compare(handA, handB);

        String output =
            "PLAYER A: " + handA.name +
            "\nPLAYER B: " + handB.name +
            "\n\n";

        if (winner > 0) {
            output += "WINNER: PLAYER A";
        }
        else if (winner < 0) {
            output += "WINNER: PLAYER B";
        }
        else {
            output += "RESULT: TIE";
        }

        result.setText(output);
    }
        }
    
