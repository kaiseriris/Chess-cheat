package com.cart3r.chessanalyzer.engine;

/** Heuristic short explanations for a move. Not ML — pattern-based. */
public final class MoveExplainer {
    private MoveExplainer() {}

    public static String explain(Board board, int move) {
        if (move == 0) return "No legal move.";
        int from = Move.from(move), to = Move.to(move), flags = Move.flags(move);
        int piece = board.squares[from];
        int type = Board.typeOf(piece);
        StringBuilder sb = new StringBuilder();

        if (flags == Move.FLAG_KING_CASTLE) { sb.append("Castles kingside, tucking the king to safety."); return sb.toString(); }
        if (flags == Move.FLAG_QUEEN_CASTLE) { sb.append("Castles queenside, activating the rook and sheltering the king."); return sb.toString(); }

        if (Move.isPromotion(move)) {
            sb.append("Promotes the pawn to a ");
            switch (Move.promoType(move)) {
                case 2: sb.append("knight"); break;
                case 3: sb.append("bishop"); break;
                case 4: sb.append("rook"); break;
                default: sb.append("queen"); break;
            }
            sb.append(". ");
        }

        int captured = board.squares[to];
        if (flags == Move.FLAG_EP) captured = -1;
        if (captured != 0 || flags == Move.FLAG_EP) {
            if (captured == -1) sb.append("Captures en passant. ");
            else {
                sb.append("Captures the ");
                sb.append(pieceName(Board.typeOf(captured)));
                sb.append(". ");
            }
        }

        // Fork detection: after move, does the moved piece attack 2+ enemy non-pawns?
        Board copy = board.copy();
        copy.makeMove(move);
        int attacks = countValuableAttacks(copy, to, Board.colorOf(piece));
        if (attacks >= 2 && type != 1) sb.append("Creates a fork. ");

        // Check
        if (copy.inCheck(copy.sideToMove)) {
            MoveGenerator.List legal = new MoveGenerator.List();
            MoveGenerator.generateLegal(copy, legal);
            if (legal.size == 0) sb.append("Checkmate. ");
            else sb.append("Delivers check. ");
        }

        if (sb.length() == 0) {
            // Quiet-move heuristics
            if (type == 2 || type == 3) {
                int homeRank = Board.colorOf(piece) == Board.WHITE ? 0 : 7;
                if ((from >> 3) == homeRank) sb.append("Develops the ");
                else sb.append("Repositions the ");
                sb.append(pieceName(type));
                sb.append(" toward the center. ");
            } else if (type == 1) {
                sb.append("Advances the pawn to claim space. ");
            } else if (type == 4) {
                if (isOpenFile(copy, to & 7)) sb.append("Places the rook on an open file. ");
                else sb.append("Repositions the rook. ");
            } else if (type == 5) {
                sb.append("Repositions the queen to a more active square. ");
            } else if (type == 6) {
                sb.append("Improves the king's position. ");
            } else {
                sb.append("Improves piece activity. ");
            }
        }
        return sb.toString().trim();
    }

    private static boolean isOpenFile(Board b, int file) {
        for (int r = 0; r < 8; r++) {
            int p = b.squares[r * 8 + file];
            if (p != 0 && Board.typeOf(p) == 1) return false;
        }
        return true;
    }

    private static int countValuableAttacks(Board b, int sq, int side) {
        int count = 0;
        int[][] D = {{-2,-1},{-2,1},{-1,-2},{-1,2},{1,-2},{1,2},{2,-1},{2,1},
                     {-1,-1},{-1,1},{1,-1},{1,1},{-1,0},{1,0},{0,-1},{0,1}};
        int r = sq >> 3, f = sq & 7;
        for (int[] d : D) {
            int nr = r + d[0], nf = f + d[1];
            int steps = Math.max(Math.abs(d[0]), Math.abs(d[1]));
            while (nr >= 0 && nr < 8 && nf >= 0 && nf < 8) {
                int p = b.squares[nr*8 + nf];
                if (p != 0) {
                    if (Board.colorOf(p) != side && Board.typeOf(p) != 6 && Board.typeOf(p) != 1) count++;
                    break;
                }
                nr += d[0]; nf += d[1];
                if (steps == 1) break;
            }
        }
        return count;
    }

    private static String pieceName(int type) {
        switch (type) {
            case 1: return "pawn";
            case 2: return "knight";
            case 3: return "bishop";
            case 4: return "rook";
            case 5: return "queen";
            case 6: return "king";
            default: return "piece";
        }
    }
}
