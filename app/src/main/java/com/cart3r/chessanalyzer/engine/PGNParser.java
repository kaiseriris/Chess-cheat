package com.cart3r.chessanalyzer.engine;

import java.util.ArrayList;
import java.util.List;

public final class PGNParser {
    public static final class Game {
        public final List<String> sans = new ArrayList<>();
        public String result = "*";
    }

    public static Game parse(String pgn) {
        Game g = new Game();
        String body = stripComments(pgn);
        String[] tokens = body.split("\\s+");
        for (String t : tokens) {
            if (t.isEmpty()) continue;
            if (t.startsWith("[")) continue;
            if (t.matches("\\d+\\.+")) continue;
            if (t.equals("1-0") || t.equals("0-1") || t.equals("1/2-1/2") || t.equals("*")) {
                g.result = t; break;
            }
            // Strip trailing ! ? + #
            g.sans.add(t);
        }
        return g;
    }

    private static String stripComments(String pgn) {
        StringBuilder sb = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < pgn.length(); i++) {
            char c = pgn.charAt(i);
            if (c == '{') { depth++; continue; }
            if (c == '}') { depth = Math.max(0, depth - 1); continue; }
            if (depth > 0) continue;
            sb.append(c);
        }
        return sb.toString();
    }
}
