package com.cart3r.chessanalyzer.engine;

public final class Evaluator {
    public static final int MATE_SCORE = 30_000;
    public static final int INFINITY   = 40_000;

    private static final int[] PIECE_VALUE = {0, 100, 320, 330, 500, 900, 20000};

    // PSTs indexed from White's view; Black mirrors with sq ^ 56.
    private static final int[] PAWN_PST = {
         0,  0,  0,  0,  0,  0,  0,  0,
         5, 10, 10,-20,-20, 10, 10,  5,
         5, -5,-10,  0,  0,-10, -5,  5,
         0,  0,  0, 20, 20,  0,  0,  0,
         5,  5, 10, 25, 25, 10,  5,  5,
        10, 10, 20, 30, 30, 20, 10, 10,
        50, 50, 50, 50, 50, 50, 50, 50,
         0,  0,  0,  0,  0,  0,  0,  0
    };
    private static final int[] KNIGHT_PST = {
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    };
    private static final int[] BISHOP_PST = {
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    };
    private static final int[] ROOK_PST = {
         0,  0,  5, 10, 10,  5,  0,  0,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
         5, 10, 10, 10, 10, 10, 10,  5,
         0,  0,  0,  0,  0,  0,  0,  0
    };
    private static final int[] QUEEN_PST = {
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -10,  5,  5,  5,  5,  5,  0,-10,
          0,  0,  5,  5,  5,  5,  0, -5,
         -5,  0,  5,  5,  5,  5,  0, -5,
        -10,  0,  5,  5,  5,  5,  0,-10,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    };
    private static final int[] KING_MID_PST = {
         20, 30, 10,  0,  0, 10, 30, 20,
         20, 20,  0,  0,  0,  0, 20, 20,
        -10,-20,-20,-20,-20,-20,-20,-10,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30
    };
    private static final int[] KING_END_PST = {
        -50,-30,-30,-30,-30,-30,-30,-50,
        -30,-30,  0,  0,  0,  0,-30,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-10, 30, 40, 40, 30,-10,-30,
        -30,-10, 30, 40, 40, 30,-10,-30,
        -30,-10, 20, 30, 30, 20,-10,-30,
        -30,-20,-10,  0,  0,-10,-20,-30,
        -50,-40,-30,-20,-20,-30,-40,-50
    };

    private Evaluator() {}

    /** Returns static eval in centipawns from the side-to-move's perspective. */
    public static int evaluate(Board b) {
        int whiteScore = 0, blackScore = 0;
        int whiteMaterial = 0, blackMaterial = 0;
        int whiteBishops = 0, blackBishops = 0;
        int[] whitePawnFiles = new int[8];
        int[] blackPawnFiles = new int[8];
        int[] whitePawnRanks = new int[8];
        int[] blackPawnRanks = new int[8];

        for (int sq = 0; sq < 64; sq++) {
            int p = b.squares[sq];
            if (p == 0) continue;
            int side = Board.colorOf(p);
            int type = Board.typeOf(p);
            int idx = (side == Board.WHITE) ? sq : (sq ^ 56);
            int val = PIECE_VALUE[type];
            int pst = 0;
            switch (type) {
                case 1: pst = PAWN_PST[idx]; break;
                case 2: pst = KNIGHT_PST[idx]; break;
                case 3: pst = BISHOP_PST[idx]; break;
                case 4: pst = ROOK_PST[idx]; break;
                case 5: pst = QUEEN_PST[idx]; break;
                case 6: pst = KING_MID_PST[idx]; break;
            }
            int total = val + pst;
            if (side == Board.WHITE) {
                whiteScore += total;
                whiteMaterial += val;
                if (type == 3) whiteBishops++;
                if (type == 1) { whitePawnFiles[sq & 7]++; whitePawnRanks[sq & 7] = (sq >> 3); }
            } else {
                blackScore += total;
                blackMaterial += val;
                if (type == 3) blackBishops++;
                if (type == 1) { blackPawnFiles[sq & 7]++; blackPawnRanks[sq & 7] = (sq >> 3); }
            }
        }

        // Bishop pair
        if (whiteBishops >= 2) whiteScore += 30;
        if (blackBishops >= 2) blackScore += 30;

        // Pawn structure
        for (int f = 0; f < 8; f++) {
            if (whitePawnFiles[f] > 1) whiteScore -= 15 * (whitePawnFiles[f] - 1);
            if (blackPawnFiles[f] > 1) blackScore -= 15 * (blackPawnFiles[f] - 1);
            boolean wIso = whitePawnFiles[f] > 0
                    && (f == 0 || whitePawnFiles[f-1] == 0)
                    && (f == 7 || whitePawnFiles[f+1] == 0);
            if (wIso) whiteScore -= 12;
            boolean bIso = blackPawnFiles[f] > 0
                    && (f == 0 || blackPawnFiles[f-1] == 0)
                    && (f == 7 || blackPawnFiles[f+1] == 0);
            if (bIso) blackScore -= 12;

            // Passed pawns
            if (whitePawnFiles[f] > 0) {
                boolean passed = true;
                for (int df = -1; df <= 1; df++) {
                    int nf = f + df;
                    if (nf < 0 || nf > 7) continue;
                    if (blackPawnFiles[nf] > 0 && blackPawnRanks[nf] > whitePawnRanks[f]) passed = false;
                }
                if (passed) {
                    int rank = whitePawnRanks[f];
                    whiteScore += 15 + 10 * rank;
                }
            }
            if (blackPawnFiles[f] > 0) {
                boolean passed = true;
                for (int df = -1; df <= 1; df++) {
                    int nf = f + df;
                    if (nf < 0 || nf > 7) continue;
                    if (whitePawnFiles[nf] > 0 && whitePawnRanks[nf] < blackPawnRanks[f]) passed = false;
                }
                if (passed) {
                    int rank = 7 - blackPawnRanks[f];
                    blackScore += 15 + 10 * rank;
                }
            }
        }

        // Endgame king PST: if material low, use KING_END_PST
        int totalMat = whiteMaterial + blackMaterial;
        if (totalMat < 2400) { // roughly endgame
            int wkSq = b.kingSquare(Board.WHITE);
            int bkSq = b.kingSquare(Board.BLACK);
            whiteScore += KING_END_PST[wkSq] - KING_MID_PST[wkSq];
            blackScore += KING_END_PST[bkSq ^ 56] - KING_MID_PST[bkSq ^ 56];
        }

        // Side to move perspective
        int diff = whiteScore - blackScore;
        int stmScore = (b.sideToMove == Board.WHITE) ? diff : -diff;
        return stmScore + 10; // tempo bonus
    }
}
