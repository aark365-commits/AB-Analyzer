package com.ab.analyzer;

import java.util.*;

public final class HandEngine {
    private HandEngine() {}

    private static final class Hand {
        final int category;
        final int[] tie;
        final String label;
        Hand(int category, String label, int... tie) {
            this.category = category;
            this.label = label;
            this.tie = tie;
        }
    }

    private static int rank(String value) {
        switch (value) {
            case "A": return 14;
            case "K": return 13;
            case "Q": return 12;
            case "J": return 11;
            default: return Integer.parseInt(value);
        }
    }

    private static Hand parse(String input) {
        String[] cards = input.toUpperCase(Locale.ROOT).trim().split("[,\\s]+");
        if (cards.length != 3) throw new IllegalArgumentException("Har player ke exactly 3 cards likho.");
        int[] ranks = new int[3];
        String[] suits = new String[3];
        Set<String> unique = new HashSet<>();

        for (int i = 0; i < 3; i++) {
            String card = cards[i];
            if (card.length() < 2) throw new IllegalArgumentException("Invalid card: " + card);
            String r = card.substring(0, card.length() - 1);
            String s = card.substring(card.length() - 1);
            if (!Arrays.asList("A","K","Q","J","10","9","8","7","6","5","4","3","2").contains(r))
                throw new IllegalArgumentException("Invalid rank: " + r);
            if (!Arrays.asList("S","H","D","C").contains(s))
                throw new IllegalArgumentException("Invalid suit: " + s);
            if (!unique.add(card)) throw new IllegalArgumentException("Duplicate card: " + card);
            ranks[i] = rank(r);
            suits[i] = s;
        }

        Arrays.sort(ranks);
        boolean trail = ranks[0] == ranks[2];
        boolean color = suits[0].equals(suits[1]) && suits[1].equals(suits[2]);
        int sequence = sequenceValue(ranks);

        if (trail) return new Hand(6, "TRAIL", ranks[2]);
        if (sequence > 0 && color) return new Hand(5, "PURE SEQUENCE", sequence);
        if (sequence > 0) return new Hand(4, "SEQUENCE", sequence);
        if (color) return new Hand(3, "COLOR", ranks[2], ranks[1], ranks[0]);
        if (ranks[0] == ranks[1]) return new Hand(2, "PAIR", ranks[0], ranks[2]);
        if (ranks[1] == ranks[2]) return new Hand(2, "PAIR", ranks[1], ranks[0]);
        return new Hand(1, "HIGH CARD", ranks[2], ranks[1], ranks[0]);
    }

    private static int sequenceValue(int[] r) {
        if (r[0] == 12 && r[1] == 13 && r[2] == 14) return 15; // Q-K-A
        if (r[0] == 2 && r[1] == 3 && r[2] == 14) return 14;  // A-2-3
        if (r[0] + 1 == r[1] && r[1] + 1 == r[2]) return r[2];
        return 0;
    }

    private static int compare(Hand a, Hand b) {
        if (a.category != b.category) return Integer.compare(a.category, b.category);
        for (int i = 0; i < Math.min(a.tie.length, b.tie.length); i++) {
            if (a.tie[i] != b.tie[i]) return Integer.compare(a.tie[i], b.tie[i]);
        }
        return 0;
    }

    public static String winner(String a, String b, String c) {
        try {
            Hand[] hands = {parse(a), parse(b), parse(c)};
            int best = 0;
            for (int i = 1; i < 3; i++) if (compare(hands[i], hands[best]) > 0) best = i;
            int tied = 0;
            for (Hand hand : hands) if (compare(hand, hands[best]) == 0) tied++;
            if (tied > 1) return "TIE";
            return new String[]{"A", "B", "C"}[best];
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public static String describe(String a, String b, String c) {
        try {
            Hand[] hands = {parse(a), parse(b), parse(c)};
            int best = 0;
            for (int i = 1; i < 3; i++) if (compare(hands[i], hands[best]) > 0) best = i;
            StringBuilder out = new StringBuilder("AB ANALYZER RESULTS\n\n");
            String[] names = {"PLAYER A", "PLAYER B", "PLAYER C"};
            for (int i = 0; i < 3; i++) {
                out.append(names[i]).append(": ").append(hands[i].label).append("\n");
            }
            int tied = 0;
            for (Hand hand : hands) if (compare(hand, hands[best]) == 0) tied++;
            out.append("\n");
            if (tied > 1) out.append("RESULT: TIE");
            else out.append("WINNER: ").append(names[best]).append(" — ").append(hands[best].label);
            return out.toString();
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}
