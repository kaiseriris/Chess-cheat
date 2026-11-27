package com.cart3r.chessanalyzer.data;

import com.cart3r.chessanalyzer.engine.ChessEngine;

public final class AnalysisResult {
    public final String fen;
    public final String bestMoveSan;
    public final String bestMoveUci;
    public final String explanation;
    public final String scoreText;
    public final int scoreCp;
    public final int depth;
    public final long nodes;
    public final java.util.List<String> altSans;
    public final java.util.List<String> altScores;

    public AnalysisResult(ChessEngine.Analysis a) {
        this.fen = a.fen;
        this.bestMoveSan = a.bestMoveSan;
        this.bestMoveUci = a.bestMoveUci;
        this.explanation = a.bestExplanation;
        this.scoreText = a.scoreText;
        this.scoreCp = a.scoreCp;
        this.depth = a.depth;
        this.nodes = a.nodes;
        this.altSans = a.altSans;
        this.altScores = new java.util.ArrayList<>();
        for (int s : a.altScores) this.altScores.add(format(s));
    }

    private static String format(int cp) {
        double p = cp / 100.0;
        return (p >= 0 ? "+" : "") + String.format(java.util.Locale.US, "%.2f", p);
    }
}
