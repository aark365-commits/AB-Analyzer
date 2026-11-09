
package com.ab.analyzer;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import android.text.InputType;
import java.util.*;

public class MainActivity extends Activity {

    EditText playerA, playerB, playerC;
    TextView result;

    static class Hand {
        int type;
        int[] tie;
        String name;
        String cards;

        Hand(int type, int[] tie, String name, String cards) {
            this.type = type;
            this.tie = tie;
            this.name = name;
            this.cards = cards;
        }
    }

    int rankValue(String r) {
        if (r.equals("A")) return 14;
        if (r.equals("K")) return 13;
        if (r.equals("Q")) return 12;
        if (r.equals("J")) return 11;
        return Integer.parseInt(r);
    }

    Hand analyze(String input) {

        String[] cards = input.toUpperCase().trim().split("[,\\s]+");

        if (cards.length != 3)
            throw new IllegalArgumentException(
                "Har player ke exactly 3 cards likho."
            );

        int[] ranks = new int[3];
        String[] suits = new String[3];
        Set<String> unique = new HashSet<>();

        for (int i = 0; i < 3; i++) {
            String c = cards[i].trim();

            if (c.length() < 2)
                throw new IllegalArgumentException("Invalid card: " + c);

            String r = c.substring(0, c.length() - 1);
            String s = c.substring(c.length() - 1);

            if (!Arrays.asList("A","K","Q","J","10","9","8",
                    "7","6","5","4","3","2").contains(r))
                throw new IllegalArgumentException("Invalid rank: " + r);

            if (!Arrays.asList("S","H","D","C").contains(s))
                throw new IllegalArgumentException("Invalid suit: " + s);

            if (!unique.add(c))
                throw new IllegalArgumentException("Duplicate card: " + c);

            ranks[i] = rankValue(r);
            suits[i] = s;
        }

        Arrays.sort(ranks);

        boolean trail = ranks[0] == ranks[2];
        boolean color = suits[0].equals(suits[1])
                     && suits[1].equals(suits[2]);

        int sequence = sequenceValue(ranks);

        int high = ranks[2];
        int middle = ranks[1];
        int low = ranks[0];

        if (trail)
            return new Hand(6, new int[]{high}, "TRAIL", input);

        if (sequence > 0 && color)
            return new Hand(5, new int[]{sequence},
                    "PURE SEQUENCE", input);

        if (sequence > 0)
            return new Hand(4, new int[]{sequence},
                    "SEQUENCE", input);

        if (color)
            return new Hand(3, new int[]{high,middle,low},
                    "COLOR", input);

        if (ranks[0] == ranks[1])
            return new Hand(2, new int[]{ranks[0],ranks[2]},
                    "PAIR", input);

        if (ranks[1] == ranks[2])
            return new Hand(2, new int[]{ranks[1],ranks[0]},
                    "PAIR", input);

        return new Hand(1, new int[]{high,middle,low},
                "HIGH CARD", input);
    }

    int sequenceValue(int[] r) {

        if (r[0] == 2 && r[1] == 3 && r[2] == 14)
            return 15;

        if (r[0] == 12 && r[1] == 13 && r[2] == 14)
            return 14;

        if (r[0] + 1 == r[1] && r[1] + 1 == r[2])
            return r[2];

        return 0;
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

    void analyzePlayers() {

        try {
            Hand a = analyze(playerA.getText().toString());
            Hand b = analyze(playerB.getText().toString());
            Hand c = analyze(playerC.getText().toString());

            String[] all = {
                playerA.getText().toString(),
                playerB.getText().toString(),
                playerC.getText().toString()
            };

            Set<String> used = new HashSet<>();

            for (String hand : all) {
                for (String card : hand.toUpperCase().trim().split("[,\\s]+")) {
                    if (!used.add(card))
                        throw new IllegalArgumentException(
                            "Same card multiple players mein use nahi ho sakta: " + card
                        );
                }
            }

            Hand[] hands = {a,b,c};
            String[] names = {"PLAYER A","PLAYER B","PLAYER C"};

            int best = 0;

            for (int i = 1; i < 3; i++) {
                if (compare(hands[i], hands[best]) > 0)
                    best = i;
            }

            StringBuilder output = new StringBuilder();

            output.append("AB ANALYZER RESULTS\n\n");

            for (int i = 0; i < 3; i++) {
                output.append(names[i]).append("\n");
                output.append("Cards: ").append(hands[i].cards).append("\n");
                output.append("Hand: ").append(hands[i].name).append("\n\n");
            }

            output.append("--------------------\n");

            boolean tie = true;

            for (int i = 0; i < 3; i++) {
                if (compare(hands[i], hands[best]) != 0)
                    tie = false;
            }

            if (tie) {
                output.append("RESULT: THREE WAY TIE");
            } else {
                output.append("WINNER: ").append(names[best]);
                output.append("\nWinning Hand: ").append(hands[best].name);
            }

            result.setText(output.toString());

        } catch (Exception e) {
            result.setText("ERROR: " + e.getMessage());
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(25,30,25,30);
        layout.setBackgroundColor(Color.rgb(18,22,32));

        scroll.addView(layout);

        TextView title = new TextView(this);
        title.setText("AB ANALYZER");
        title.setTextSize(30);
        title.setTextColor(Color.WHITE);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        TextView info = new TextView(this);
        info.setText("\nEnter 3 cards for each player.\nExample: AS KH 2D\nS=Spades H=Hearts D=Diamonds C=Clubs");
        info.setTextColor(Color.LTGRAY);
        info.setTextSize(15);
        layout.addView(info);

        playerA = addPlayer(layout, "PLAYER A");
        playerB = addPlayer(layout, "PLAYER B");
        playerC = addPlayer(layout, "PLAYER C");

        Button button = new Button(this);
        button.setText("ANALYZE ALL 3 PLAYERS");
        layout.addView(button);

        result = new TextView(this);
        result.setTextColor(Color.WHITE);
        result.setTextSize(18);
        result.setPadding(10,30,10,20);
        layout.addView(result);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                analyzePlayers();
            }
        });

        setContentView(scroll);
    }

    EditText addPlayer(LinearLayout layout, String name) {

        TextView label = new TextView(this);
        label.setText("\n" + name);
        label.setTextColor(Color.WHITE);
        label.setTextSize(18);
        layout.addView(label);

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Example: AS KH 2D");
        input.setHintTextColor(Color.GRAY);
        input.setTextColor(Color.WHITE);
        input.setTextSize(18);
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        layout.addView(input);

        return input;
    }
            }
                
