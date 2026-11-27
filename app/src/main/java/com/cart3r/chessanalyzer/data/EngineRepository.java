package com.cart3r.chessanalyzer.data;

import android.os.Handler;
import android.os.Looper;

import com.cart3r.chessanalyzer.engine.Board;
import com.cart3r.chessanalyzer.engine.ChessEngine;
import com.cart3r.chessanalyzer.engine.FENParser;
import com.cart3r.chessanalyzer.engine.PGNParser;
import com.cart3r.chessanalyzer.engine.SAN;
import com.cart3r.chessanalyzer.engine.Search;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class EngineRepository {
    public interface Callback {
        void onProgress(int depth, int scoreCp, long nodes);
        void onComplete(AnalysisResult result);
        void onError(String message);
    }

    private final ChessEngine engine = new ChessEngine();
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean busy = false;

    public boolean isBusy() { return busy; }

    public void analyzeFen(String fen, int maxDepth, long timeMs, Callback cb) {
        analyzeInternal(() -> FENParser.parse(fen), maxDepth, timeMs, cb);
    }

    public void analyzePgn(String pgn, int maxDepth, long timeMs, Callback cb) {
        analyzeInternal(() -> {
            PGNParser.Game g = PGNParser.parse(pgn);
            Board board = FENParser.parse("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
            for (String san : g.sans) {
                int mv = findMoveFromSan(board, san);
                if (mv == 0) break;
                board.makeMove(mv);
            }
            return board;
        }, maxDepth, timeMs, cb);
    }

    public void analyzeMoves(String moveList, int maxDepth, long timeMs, Callback cb) {
        analyzeInternal(() -> {
            Board board = FENParser.parse("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
            String[] tokens = moveList.trim().split("\\s+");
            for (String t : tokens) {
                if (t.isEmpty()) continue;
                int mv = findMoveFromSan(board, t);
                if (mv == 0) break;
                board.makeMove(mv);
            }
            return board;
        }, maxDepth, timeMs, cb);
    }

    private void analyzeInternal(BoardProvider provider, int depth, long timeMs, Callback cb) {
        if (busy) { cb.onError("Engine busy"); return; }
        busy = true;
        exec.execute(() -> {
            try {
                Board board = provider.get();
                ChessEngine.Analysis a = engine.analyze(board, depth, timeMs,
                        (d, s, m, n) -> main.post(() -> cb.onProgress(d, s, n)));
                AnalysisResult r = new AnalysisResult(a);
                main.post(() -> { busy = false; cb.onComplete(r); });
            } catch (Exception e) {
                busy = false;
                main.post(() -> cb.onError(e.getMessage() == null ? "Analysis failed" : e.getMessage()));
            }
        });
    }

    private interface BoardProvider { Board get() throws Exception; }

    private static int findMoveFromSan(Board board, String san) {
        com.cart3r.chessanalyzer.engine.MoveGenerator.List legal =
                new com.cart3r.chessanalyzer.engine.MoveGenerator.List();
        com.cart3r.chessanalyzer.engine.MoveGenerator.generateLegal(board, legal);
        for (int i = 0; i < legal.size; i++) {
            int m = legal.get(i);
            if (SAN.toSAN(board.copy(), m).equals(san)) return m;
        }
        return 0;
    }
}
