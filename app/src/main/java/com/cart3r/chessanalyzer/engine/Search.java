package com.cart3r.chessanalyzer.engine;

import java.util.ArrayList;
import java.util.List;

public final class Search {
    public interface Listener {
        void onDepthCompleted(int depth, int score, int bestMove, long nodes);
    }

    public static final class RootMove {
        public int move;
        public int score;
    }

    private final TranspositionTable tt;
    private final MoveGenerator.List moveBuf = new MoveGenerator.List();
    private final int[][] killers = new int[64][2];
    private final int[][] history = new int[2][64 * 64];
    private final long[] nodes = {0};
    private long deadline;
    private volatile boolean abort = false;
    private boolean useTt = true;

    public Search() { tt = new TranspositionTable(20); }

    public TranspositionTable tt() { return tt; }
    public void clearTt() { tt.clear(); }
    public void abort() { abort = true; }
    public void resetAbort() { abort = false; }

    public static final class Result {
        public int bestMove;
        public int score;
        public int depth;
        public long nodes;
        public final List<RootMove> topMoves = new ArrayList<>();
    }

    /** Run iterative deepening. Returns top moves. */
    public Result search(Board board, int maxDepth, long maxTimeMs, Listener listener) {
        deadline = System.currentTimeMillis() + maxTimeMs;
        nodes[0] = 0;
        abort = false;
        tt.newSearch();
        for (int[] k : killers) { k[0] = 0; k[1] = 0; }
        for (int[] h : history) java.util.Arrays.fill(h, 0);

        Result res = new Result();
        List<Integer> rootMoves = new ArrayList<>();
        List<MoveGenerator.List> unused = null;
        // Collect root moves once
        MoveGenerator.List rl = new MoveGenerator.List();
        MoveGenerator.generateLegal(board, rl);
        if (rl.size == 0) {
            res.bestMove = 0; res.score = board.inCheck(board.sideToMove) ? -Evaluator.MATE_SCORE : 0;
            return res;
        }
        for (int i = 0; i < rl.size; i++) rootMoves.add(rl.get(i));

        int bestMoveEver = rootMoves.get(0);
        int bestScoreEver = 0;
        int depthReached = 0;
        List<RootMove> topEver = new ArrayList<>();

        for (int d = 1; d <= maxDepth; d++) {
            List<RootMove> iterScores = new ArrayList<>();
            int alpha = -Evaluator.INFINITY;
            int beta  =  Evaluator.INFINITY;
            int bestScore = -Evaluator.INFINITY;
            int bestMove = 0;

            for (int rm : rootMoves) {
                if (abort) break;
                board.makeMove(rm);
                int score = -negamax(board, d - 1, -beta, -alpha, 1);
                board.unmakeMove(rm);
                if (abort) break;
                RootMove r = new RootMove();
                r.move = rm; r.score = score;
                iterScores.add(r);
                if (score > bestScore) { bestScore = score; bestMove = rm; }
                if (score > alpha) alpha = score;
            }
            if (abort) break;
            // Sort next-iteration order by this depth's score
            iterScores.sort((a, b2) -> Integer.compare(b2.score, a.score));
            rootMoves.clear();
            for (RootMove r : iterScores) rootMoves.add(r.move);

            bestMoveEver = bestMove;
            bestScoreEver = bestScore;
            depthReached = d;
            topEver = iterScores;
            if (listener != null) listener.onDepthCompleted(d, bestScore, bestMove, nodes[0]);
            if (Math.abs(bestScore) > Evaluator.MATE_SCORE - 100) break;
        }

        res.bestMove = bestMoveEver;
        res.score = bestScoreEver;
        res.depth = depthReached;
        res.nodes = nodes[0];
        if (!topEver.isEmpty()) {
            for (int i = 0; i < Math.min(topEver.size(), 3); i++) res.topMoves.add(topEver.get(i));
        }
        return res;
    }

    private int negamax(Board b, int depth, int alpha, int beta, int ply) {
        if ((++nodes[0] & 2047) == 0) {
            if (System.currentTimeMillis() > deadline) { abort = true; return 0; }
        }
        if (abort) return 0;

        // TT probe
        long key = b.zobristHash;
        TranspositionTable.Entry tte = useTt ? tt.probe(key) : null;
        int ttMove = 0;
        if (tte != null) {
            ttMove = tte.move;
            if (tte.depth >= depth) {
                int s = tte.score;
                if (s > Evaluator.MATE_SCORE - 500) s -= ply;
                else if (s < -Evaluator.MATE_SCORE + 500) s += ply;
                if (tte.flag == TranspositionTable.EXACT) return s;
                if (tte.flag == TranspositionTable.LOWER && s >= beta) return s;
                if (tte.flag == TranspositionTable.UPPER && s <= alpha) return s;
            }
        }

        // Mate distance pruning
        int mateValue = Evaluator.MATE_SCORE - ply;
        if (alpha < -mateValue) alpha = -mateValue;
        if (beta > mateValue - 1) beta = mateValue - 1;
        if (alpha >= beta) return alpha;

        // Draw detection: fifty-move / repetition (simplified)
        if (b.halfmoveClock >= 100) return 0;

        if (depth <= 0) return quiescence(b, alpha, beta, ply);

        boolean inCheck = b.inCheck(b.sideToMove);
        if (inCheck) depth++; // check extension

        // Null move pruning
        if (depth >= 3 && !inCheck && hasNonPawnMaterial(b, b.sideToMove)) {
            // Make a null move
            b.sideToMove ^= 1;
            b.zobristHash ^= Zobrist.sideKey;
            b.epSquare = -1;
            int nullScore = -negamax(b, depth - 1 - 2, -beta, -beta + 1, ply + 1);
            b.sideToMove ^= 1;
            b.zobristHash ^= Zobrist.sideKey;
            if (abort) return 0;
            if (nullScore >= beta) return beta;
        }

        MoveGenerator.generatePseudoLegal(b, moveBuf);
        orderMoves(b, moveBuf, ttMove, ply);

        int bestScore = -Evaluator.INFINITY;
        int bestMove = 0;
        int legalCount = 0;
        int originalAlpha = alpha;

        for (int i = 0; i < moveBuf.size; i++) {
            int m = moveBuf.get(i);
            b.makeMove(m);
            // legality
            if (b.isSquareAttacked(b.kingSquare(b.sideToMove ^ 1), b.sideToMove)) {
                b.unmakeMove(m);
                continue;
            }
            legalCount++;
            int score = -negamax(b, depth - 1, -beta, -alpha, ply + 1);
            b.unmakeMove(m);
            if (abort) return 0;
            if (score > bestScore) { bestScore = score; bestMove = m; }
            if (score > alpha) alpha = score;
            if (alpha >= beta) {
                if (!Move.isCapture(m, b)) {
                    killers[ply][1] = killers[ply][0];
                    killers[ply][0] = m;
                    history[b.sideToMove][Move.from(m) * 64 + Move.to(m)] += depth * depth;
                }
                break;
            }
        }

        if (legalCount == 0) {
            return inCheck ? (-Evaluator.MATE_SCORE + ply) : 0;
        }

        int storeScore = bestScore;
        if (storeScore > Evaluator.MATE_SCORE - 500) storeScore += ply;
        else if (storeScore < -Evaluator.MATE_SCORE + 500) storeScore -= ply;

        if (useTt) {
            int flag = (bestScore <= originalAlpha) ? TranspositionTable.UPPER
                    : (bestScore >= beta) ? TranspositionTable.LOWER
                    : TranspositionTable.EXACT;
            tt.store(key, depth, storeScore, bestMove, flag);
        }
        return bestScore;
    }

    private int quiescence(Board b, int alpha, int beta, int ply) {
        if ((++nodes[0] & 2047) == 0) {
            if (System.currentTimeMillis() > deadline) { abort = true; return 0; }
        }
        if (abort) return 0;

        int standPat = Evaluator.evaluate(b);
        if (standPat >= beta) return beta;
        if (standPat > alpha) alpha = standPat;

        MoveGenerator.generatePseudoLegal(b, moveBuf);
        // Only captures / promotions
        int n = 0;
        int[] tmp = moveBuf.moves;
        for (int i = 0; i < moveBuf.size; i++) {
            int m = tmp[i];
            if (Move.isCapture(m, b) || Move.isPromotion(m)) tmp[n++] = m;
        }
        moveBuf.size = n;
        orderMoves(b, moveBuf, 0, ply);

        for (int i = 0; i < moveBuf.size; i++) {
            int m = moveBuf.get(i);
            b.makeMove(m);
            if (b.isSquareAttacked(b.kingSquare(b.sideToMove ^ 1), b.sideToMove)) {
                b.unmakeMove(m); continue;
            }
            int score = -quiescence(b, -beta, -alpha, ply + 1);
            b.unmakeMove(m);
            if (abort) return 0;
            if (score >= beta) return beta;
            if (score > alpha) alpha = score;
        }
        return alpha;
    }

    private void orderMoves(Board b, MoveGenerator.List list, int ttMove, int ply) {
        int n = list.size;
        int[] arr = list.moves;
        int[] scores = new int[n];
        int k1 = killers[ply][0], k2 = killers[ply][1];
        for (int i = 0; i < n; i++) {
            int m = arr[i];
            int s = 0;
            if (m == ttMove) s = 1_000_000;
            else if (Move.isCapture(m, b)) {
                int victim = Board.typeOf(b.squares[Move.to(m)]);
                int attacker = Board.typeOf(b.squares[Move.from(m)]);
                if (victim == 0 && Move.flags(m) == Move.FLAG_EP) victim = 1;
                s = 100_000 + victim * 100 - attacker;
            } else if (Move.isPromotion(m)) {
                s = 90_000 + Move.promoType(m);
            } else if (m == k1) s = 80_000;
            else if (m == k2) s = 79_000;
            else s = history[b.sideToMove][Move.from(m) * 64 + Move.to(m)];
            scores[i] = s;
        }
        // Insertion sort (small list)
        for (int i = 1; i < n; i++) {
            int m = arr[i], s = scores[i], j = i - 1;
            while (j >= 0 && scores[j] < s) {
                arr[j + 1] = arr[j]; scores[j + 1] = scores[j]; j--;
            }
            arr[j + 1] = m; scores[j + 1] = s;
        }
    }

    private static boolean hasNonPawnMaterial(Board b, int side) {
        for (int sq = 0; sq < 64; sq++) {
            int p = b.squares[sq];
            if (p == 0 || Board.colorOf(p) != side) continue;
            int t = Board.typeOf(p);
            if (t != 1 && t != 6) return true;
        }
        return false;
    }
}
