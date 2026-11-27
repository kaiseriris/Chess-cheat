package com.cart3r.chessanalyzer.engine;

public final class ChessEngine {
    private final Search search = new Search();

    public static final class Analysis {
        public String fen;
        public int bestMove;
        public String bestMoveSan;
        public String bestMoveUci;
        public String bestExplanation;
        public int scoreCp;
        public String scoreText;
        public int depth;
        public long nodes;
        public java.util.List<String> altSans = new java.util.ArrayList<>();
        public java.util.List<String> altUci = new java.util.ArrayList<>();
        public java.util.List<Integer> altScores = new java.util.ArrayList<>();
        public java.util.List<String> altExplanations = new java.util.ArrayList<>();
    }

    public Search search() { return search; }

    public Analysis analyze(Board board, int maxDepth, long maxTimeMs, Search.Listener listener) {
        Board copy = board.copy();
        Search.Result r = search.search(copy, maxDepth, maxTimeMs, listener);

        Analysis a = new Analysis();
        a.fen = FENParser.toFen(copy);
        a.bestMove = r.bestMove;
        a.scoreCp = r.score;
        a.scoreText = formatScore(r.score);
        a.depth = r.depth;
        a.nodes = r.nodes;

        if (r.bestMove != 0) {
            Board bb = copy.copy();
            a.bestMoveSan = SAN.toSAN(bb, r.bestMove);
            a.bestMoveUci = uci(bb, r.bestMove);
            a.bestExplanation = MoveExplainer.explain(bb, r.bestMove);
        } else {
            a.bestMoveSan = "—";
            a.bestMoveUci = "—";
            a.bestExplanation = copy.inCheck(copy.sideToMove)
                    ? "Checkmate — no legal moves." : "Stalemate — no legal moves.";
        }

        for (int i = 1; i < r.topMoves.size(); i++) {
            Search.RootMove rm = r.topMoves.get(i);
            Board bb = copy.copy();
            a.altSans.add(SAN.toSAN(bb, rm.move));
            a.altUci.add(uci(bb, rm.move));
            a.altScores.add(rm.score);
            a.altExplanations.add(MoveExplainer.explain(bb, rm.move));
        }
        return a;
    }

    private static String uci(Board b, int move) {
        return SAN.squareName(Move.from(move)) + SAN.squareName(Move.to(move))
                + (Move.isPromotion(move) ? String.valueOf(Character.toLowerCase("nbrq".charAt(Move.promoType(move) - 2))) : "");
    }

    private static String formatScore(int cp) {
        if (cp > Evaluator.MATE_SCORE - 500) {
            int plies = Evaluator.MATE_SCORE - cp;
            int moves = (plies + 1) / 2;
            return "MATE in " + moves;
        }
        if (cp < -Evaluator.MATE_SCORE + 500) {
            int plies = Evaluator.MATE_SCORE + cp;
            int moves = (plies + 1) / 2;
            return "MATE against in " + moves;
        }
        double pawns = cp / 100.0;
        return (pawns >= 0 ? "+" : "") + String.format(java.util.Locale.US, "%.2f", pawns);
    }
}
