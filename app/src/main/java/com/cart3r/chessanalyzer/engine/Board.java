package com.cart3r.chessanalyzer.engine;

public final class Board {
    public static final int WHITE = 0, BLACK = 1;

    // Piece: signed. +1=P +2=N +3=B +4=R +5=Q +6=K ; negative = black.
    public final int[] squares = new int[64];
    public int sideToMove = WHITE;
    public int castlingRights = 0;   // 1=WK 2=WQ 4=BK 8=BQ
    public int epSquare = -1;
    public int halfmoveClock = 0;
    public int fullmoveNumber = 1;
    public final int[] kingSquare = new int[2];
    public long zobristHash = 0L;

    private static final class Undo {
        int captured;
        int castling;
        int ep;
        int halfmove;
        long hash;
        int wKing, bKing;
    }
    private final Undo[] undoStack = new Undo[1024];
    private int undoTop = 0;
    { for (int i = 0; i < undoStack.length; i++) undoStack[i] = new Undo(); }

    public Board() {}

    public Board copy() {
        Board b = new Board();
        System.arraycopy(squares, 0, b.squares, 0, 64);
        b.sideToMove = sideToMove;
        b.castlingRights = castlingRights;
        b.epSquare = epSquare;
        b.halfmoveClock = halfmoveClock;
        b.fullmoveNumber = fullmoveNumber;
        b.kingSquare[0] = kingSquare[0];
        b.kingSquare[1] = kingSquare[1];
        b.zobristHash = zobristHash;
        return b;
    }

    public static int colorOf(int piece) { return piece > 0 ? WHITE : BLACK; }
    public static int typeOf(int piece)  { return Math.abs(piece); }

    public boolean isWhite(int sq) { return squares[sq] > 0; }
    public boolean isBlack(int sq) { return squares[sq] < 0; }
    public boolean isEmpty(int sq) { return squares[sq] == 0; }
    public boolean isEnemy(int sq, int side) {
        int p = squares[sq];
        return p != 0 && colorOf(p) != side;
    }
    public boolean isFriend(int sq, int side) {
        int p = squares[sq];
        return p != 0 && colorOf(p) == side;
    }

    public int kingSquare(int side) { return kingSquare[side]; }

    /** Recompute Zobrist + king squares from scratch. */
    public void recomputeState() {
        long h = 0L;
        for (int s = 0; s < 64; s++) {
            int p = squares[s];
            if (p == 0) continue;
            int c = colorOf(p), t = typeOf(p);
            h ^= Zobrist.pieceKeys[c][t][s];
            if (t == 6) kingSquare[c] = s;
        }
        if (castlingRights != 0) h ^= Zobrist.castlingKeys[castlingRights];
        if (epSquare >= 0) h ^= Zobrist.epKeys[epSquare & 7];
        if (sideToMove == BLACK) h ^= Zobrist.sideKey;
        zobristHash = h;
    }

    // ---------------- make / unmake ----------------
    public void makeMove(int move) {
        Undo u = undoStack[undoTop++];
        u.captured = 0;
        u.castling = castlingRights;
        u.ep = epSquare;
        u.halfmove = halfmoveClock;
        u.hash = zobristHash;
        u.wKing = kingSquare[0];
        u.bKing = kingSquare[1];

        int from = Move.from(move), to = Move.to(move), flags = Move.flags(move);
        int moving = squares[from];
        int side = colorOf(moving);
        int type = typeOf(moving);

        // Halfmove clock
        halfmoveClock = (type == 1 || squares[to] != 0 || flags == Move.FLAG_EP) ? 0 : halfmoveClock + 1;

        // Remove from
        squares[from] = 0;
        zobristHash ^= Zobrist.pieceKeys[side][type][from];

        // Handle capture
        int capturedSq = to;
        if (flags == Move.FLAG_EP) {
            capturedSq = (side == WHITE) ? to - 8 : to + 8;
        }
        if (flags == Move.FLAG_EP || (squares[to] != 0 && flags != Move.FLAG_KING_CASTLE && flags != Move.FLAG_QUEEN_CASTLE)) {
            int cap = squares[capturedSq];
            u.captured = cap;
            if (cap != 0) {
                int cSide = colorOf(cap), cType = typeOf(cap);
                squares[capturedSq] = 0;
                zobristHash ^= Zobrist.pieceKeys[cSide][cType][capturedSq];
            }
        }

        // Place on destination (possibly promoted)
        int placed = moving;
        if (Move.isPromotion(move)) {
            int promo = Move.promoType(move);
            placed = (side == WHITE) ? promo : -promo;
        }
        squares[to] = placed;
        zobristHash ^= Zobrist.pieceKeys[side][typeOf(placed)][to];
        if (type == 6) kingSquare[side] = to;

        // Castling rook
        if (flags == Move.FLAG_KING_CASTLE) {
            int rf = (side == WHITE) ? 7 : 63;
            int rt = (side == WHITE) ? 5 : 61;
            int rk = squares[rf];
            squares[rf] = 0; squares[rt] = rk;
            zobristHash ^= Zobrist.pieceKeys[side][4][rf];
            zobristHash ^= Zobrist.pieceKeys[side][4][rt];
        } else if (flags == Move.FLAG_QUEEN_CASTLE) {
            int rf = (side == WHITE) ? 0 : 56;
            int rt = (side == WHITE) ? 3 : 59;
            int rk = squares[rf];
            squares[rf] = 0; squares[rt] = rk;
            zobristHash ^= Zobrist.pieceKeys[side][4][rf];
            zobristHash ^= Zobrist.pieceKeys[side][4][rt];
        }

        // En passant target
        int newEp = -1;
        if (flags == Move.FLAG_DOUBLE_PUSH) {
            newEp = (side == WHITE) ? (from + 8) : (from - 8);
        }

        // Castling rights
        int newRights = castlingRights;
        if (from == 4  || to == 4)  newRights &= ~3;
        if (from == 60 || to == 60) newRights &= ~12;
        if (from == 0  || to == 0)  newRights &= ~2;
        if (from == 7  || to == 7)  newRights &= ~1;
        if (from == 56 || to == 56) newRights &= ~8;
        if (from == 63 || to == 63) newRights &= ~4;

        // Update hash for castling / ep
        if (castlingRights != 0) zobristHash ^= Zobrist.castlingKeys[castlingRights];
        if (newRights != 0)      zobristHash ^= Zobrist.castlingKeys[newRights];
        castlingRights = newRights;

        if (epSquare >= 0) zobristHash ^= Zobrist.epKeys[epSquare & 7];
        if (newEp >= 0)    zobristHash ^= Zobrist.epKeys[newEp & 7];
        epSquare = newEp;

        // Side flip
        sideToMove ^= 1;
        zobristHash ^= Zobrist.sideKey;
        if (side == BLACK) fullmoveNumber++;
    }

    public void unmakeMove(int move) {
        Undo u = undoStack[--undoTop];
        sideToMove ^= 1;
        if (sideToMove == BLACK) fullmoveNumber--;

        int from = Move.from(move), to = Move.to(move), flags = Move.flags(move);
        int placed = squares[to];

        squares[to] = 0;
        // For promotions, restore pawn
        if (Move.isPromotion(move)) {
            squares[from] = (sideToMove == WHITE) ? 1 : -1;
        } else {
            squares[from] = placed;
        }

        // Restore capture
        if (flags == Move.FLAG_EP) {
            int capSq = (sideToMove == WHITE) ? to - 8 : to + 8;
            squares[capSq] = u.captured;
        } else if (u.captured != 0) {
            squares[to] = u.captured;
        }

        // Castling rook
        if (flags == Move.FLAG_KING_CASTLE) {
            int rf = (sideToMove == WHITE) ? 7 : 63;
            int rt = (sideToMove == WHITE) ? 5 : 61;
            squares[rf] = squares[rt];
            squares[rt] = 0;
        } else if (flags == Move.FLAG_QUEEN_CASTLE) {
            int rf = (sideToMove == WHITE) ? 0 : 56;
            int rt = (sideToMove == WHITE) ? 3 : 59;
            squares[rf] = squares[rt];
            squares[rt] = 0;
        }

        castlingRights = u.castling;
        epSquare = u.ep;
        halfmoveClock = u.halfmove;
        zobristHash = u.hash;
        kingSquare[0] = u.wKing;
        kingSquare[1] = u.bKing;
    }

    // ---------------- attacks ----------------
    private static final int[][] KNIGHT_D = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
    private static final int[][] KING_D   = {{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}};
    private static final int[][] BISHOP_D = {{-1,-1},{-1,1},{1,-1},{1,1}};
    private static final int[][] ROOK_D   = {{-1,0},{1,0},{0,-1},{0,1}};

    /** True if `sq` is attacked by any piece of `bySide`. */
    public boolean isSquareAttacked(int sq, int bySide) {
        int sr = sq >> 3, sf = sq & 7;

        // Pawns
        if (bySide == WHITE) {
            if (sr > 0) {
                if (sf > 0 && squares[(sr-1)*8 + (sf-1)] == 1) return true;
                if (sf < 7 && squares[(sr-1)*8 + (sf+1)] == 1) return true;
            }
        } else {
            if (sr < 7) {
                if (sf > 0 && squares[(sr+1)*8 + (sf-1)] == -1) return true;
                if (sf < 7 && squares[(sr+1)*8 + (sf+1)] == -1) return true;
            }
        }

        // Knights
        int knight = (bySide == WHITE) ? 2 : -2;
        for (int[] d : KNIGHT_D) {
            int r = sr + d[0], f = sf + d[1];
            if (r >= 0 && r < 8 && f >= 0 && f < 8 && squares[r*8 + f] == knight) return true;
        }

        // King
        int king = (bySide == WHITE) ? 6 : -6;
        for (int[] d : KING_D) {
            int r = sr + d[0], f = sf + d[1];
            if (r >= 0 && r < 8 && f >= 0 && f < 8 && squares[r*8 + f] == king) return true;
        }

        // Bishops / Queens (diagonals)
        int bishop = (bySide == WHITE) ? 3 : -3;
        int queen  = (bySide == WHITE) ? 5 : -5;
        for (int[] d : BISHOP_D) {
            int r = sr + d[0], f = sf + d[1];
            while (r >= 0 && r < 8 && f >= 0 && f < 8) {
                int p = squares[r*8 + f];
                if (p != 0) {
                    if (p == bishop || p == queen) return true;
                    break;
                }
                r += d[0]; f += d[1];
            }
        }

        // Rooks / Queens (orthogonals)
        int rook = (bySide == WHITE) ? 4 : -4;
        for (int[] d : ROOK_D) {
            int r = sr + d[0], f = sf + d[1];
            while (r >= 0 && r < 8 && f >= 0 && f < 8) {
                int p = squares[r*8 + f];
                if (p != 0) {
                    if (p == rook || p == queen) return true;
                    break;
                }
                r += d[0]; f += d[1];
            }
        }

        return false;
    }

    public boolean inCheck(int side) {
        return isSquareAttacked(kingSquare[side], side ^ 1);
    }
}
