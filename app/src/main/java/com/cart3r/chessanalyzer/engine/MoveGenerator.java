package com.cart3r.chessanalyzer.engine;

public final class MoveGenerator {

    public static final class List {
        public int[] moves = new int[256];
        public int size = 0;
        public void add(int m) {
            if (size >= moves.length) {
                int[] n = new int[moves.length * 2];
                System.arraycopy(moves, 0, n, 0, size);
                moves = n;
            }
            moves[size++] = m;
        }
        public void clear() { size = 0; }
        public int get(int i) { return moves[i]; }
    }

    private MoveGenerator() {}

    /** Pseudo-legal moves (may leave own king in check). */
    public static void generatePseudoLegal(Board b, List out) {
        out.clear();
        int side = b.sideToMove;
        for (int sq = 0; sq < 64; sq++) {
            int p = b.squares[sq];
            if (p == 0) continue;
            if (Board.colorOf(p) != side) continue;
            int type = Board.typeOf(p);
            switch (type) {
                case 1: pawnMoves(b, sq, side, out); break;
                case 2: knightMoves(b, sq, side, out); break;
                case 3: sliderMoves(b, sq, side, out, true, false); break;
                case 4: sliderMoves(b, sq, side, out, false, true); break;
                case 5: sliderMoves(b, sq, side, out, true, true); break;
                case 6: kingMoves(b, sq, side, out); break;
            }
        }
        if (side == Board.WHITE) tryWhiteCastle(b, out); else tryBlackCastle(b, out);
    }

    /** Legal moves only. */
    public static void generateLegal(Board b, List out) {
        List pseudo = new List();
        generatePseudoLegal(b, pseudo);
        out.clear();
        for (int i = 0; i < pseudo.size; i++) {
            int m = pseudo.get(i);
            b.makeMove(m);
            if (!b.isSquareAttacked(b.kingSquare(b.sideToMove ^ 1), b.sideToMove)) {
                out.add(m);
            }
            b.unmakeMove(m);
        }
    }

    private static void addPawn(int to, int from, int flags, List out) {
        out.add(Move.encode(from, to, flags));
    }

    private static void pawnMoves(Board b, int sq, int side, List out) {
        int r = sq >> 3, f = sq & 7;
        int dir = (side == Board.WHITE) ? 1 : -1;
        int startRank = (side == Board.WHITE) ? 1 : 6;
        int lastRank = (side == Board.WHITE) ? 7 : 0;

        int r1 = r + dir;
        if (r1 >= 0 && r1 < 8 && b.isEmpty(r1*8 + f)) {
            if (r1 == lastRank) {
                addPromotions(sq, r1*8 + f, false, out);
            } else {
                addPawn(r1*8 + f, sq, Move.FLAG_NORMAL, out);
                if (r == startRank && b.isEmpty((r + 2*dir)*8 + f)) {
                    addPawn((r + 2*dir)*8 + f, sq, Move.FLAG_DOUBLE_PUSH, out);
                }
            }
        }

        for (int df = -1; df <= 1; df += 2) {
            int nf = f + df;
            if (nf < 0 || nf > 7) continue;
            if (r1 < 0 || r1 >= 8) continue;
            int to = r1*8 + nf;
            int target = b.squares[to];
            if (target != 0 && Board.colorOf(target) != side) {
                if (r1 == lastRank) addPromotions(sq, to, true, out);
                else addPawn(to, sq, Move.FLAG_NORMAL, out);
            } else if (b.epSquare == to) {
                addPawn(to, sq, Move.FLAG_EP, out);
            }
        }
    }

    private static void addPromotions(int from, int to, boolean capture, List out) {
        int base = capture ? Move.FLAG_PROMO_CAP_N : Move.FLAG_PROMO_N;
        for (int i = 0; i < 4; i++) {
            out.add(Move.encode(from, to, base + i));
        }
    }

    private static void knightMoves(Board b, int sq, int side, List out) {
        int r = sq >> 3, f = sq & 7;
        int[][] D = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1}};
        for (int[] d : D) {
            int nr = r + d[0], nf = f + d[1];
            if (nr < 0 || nr > 7 || nf < 0 || nf > 7) continue;
            int to = nr*8 + nf;
            if (b.squares[to] != 0 && Board.colorOf(b.squares[to]) == side) continue;
            addPawn(to, sq, Move.FLAG_NORMAL, out);
        }
    }

    private static void sliderMoves(Board b, int sq, int side, List out, boolean diag, boolean orth) {
        int r = sq >> 3, f = sq & 7;
        if (diag) {
            for (int[] d : new int[][]{{-1,-1},{-1,1},{1,-1},{1,1}}) {
                int nr = r + d[0], nf = f + d[1];
                while (nr >= 0 && nr < 8 && nf >= 0 && nf < 8) {
                    int to = nr*8 + nf;
                    int p = b.squares[to];
                    if (p == 0) { addPawn(to, sq, Move.FLAG_NORMAL, out); }
                    else {
                        if (Board.colorOf(p) != side) addPawn(to, sq, Move.FLAG_NORMAL, out);
                        break;
                    }
                    nr += d[0]; nf += d[1];
                }
            }
        }
        if (orth) {
            for (int[] d : new int[][]{{-1,0},{1,0},{0,-1},{0,1}}) {
                int nr = r + d[0], nf = f + d[1];
                while (nr >= 0 && nr < 8 && nf >= 0 && nf < 8) {
                    int to = nr*8 + nf;
                    int p = b.squares[to];
                    if (p == 0) { addPawn(to, sq, Move.FLAG_NORMAL, out); }
                    else {
                        if (Board.colorOf(p) != side) addPawn(to, sq, Move.FLAG_NORMAL, out);
                        break;
                    }
                    nr += d[0]; nf += d[1];
                }
            }
        }
    }

    private static void kingMoves(Board b, int sq, int side, List out) {
        int r = sq >> 3, f = sq & 7;
        for (int[] d : new int[][]{{-1,-1},{-1,0},{-1,1},{0,-1},{0,1},{1,-1},{1,0},{1,1}}) {
            int nr = r + d[0], nf = f + d[1];
            if (nr < 0 || nr > 7 || nf < 0 || nf > 7) continue;
            int to = nr*8 + nf;
            if (b.squares[to] != 0 && Board.colorOf(b.squares[to]) == side) continue;
            addPawn(to, sq, Move.FLAG_NORMAL, out);
        }
    }

    private static void tryWhiteCastle(Board b, List out) {
        if ((b.castlingRights & 1) != 0
                && b.isEmpty(5) && b.isEmpty(6)
                && !b.isSquareAttacked(4, Board.BLACK)
                && !b.isSquareAttacked(5, Board.BLACK)) {
            out.add(Move.encode(4, 6, Move.FLAG_KING_CASTLE));
        }
        if ((b.castlingRights & 2) != 0
                && b.isEmpty(1) && b.isEmpty(2) && b.isEmpty(3)
                && !b.isSquareAttacked(4, Board.BLACK)
                && !b.isSquareAttacked(3, Board.BLACK)) {
            out.add(Move.encode(4, 2, Move.FLAG_QUEEN_CASTLE));
        }
    }
    private static void tryBlackCastle(Board b, List out) {
        if ((b.castlingRights & 4) != 0
                && b.isEmpty(61) && b.isEmpty(62)
                && !b.isSquareAttacked(60, Board.WHITE)
                && !b.isSquareAttacked(61, Board.WHITE)) {
            out.add(Move.encode(60, 62, Move.FLAG_KING_CASTLE));
        }
        if ((b.castlingRights & 8) != 0
                && b.isEmpty(57) && b.isEmpty(58) && b.isEmpty(59)
                && !b.isSquareAttacked(60, Board.WHITE)
                && !b.isSquareAttacked(59, Board.WHITE)) {
            out.add(Move.encode(60, 58, Move.FLAG_QUEEN_CASTLE));
        }
    }
}
