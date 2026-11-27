package com.cart3r.chessanalyzer.engine;

public final class SAN {
    private SAN() {}

    public static String toSAN(Board board, int move) {
        int from = Move.from(move), to = Move.to(move), flags = Move.flags(move);
        if (flags == Move.FLAG_KING_CASTLE) return finish(board, move, "O-O");
        if (flags == Move.FLAG_QUEEN_CASTLE) return finish(board, move, "O-O-O");

        int piece = board.squares[from];
        int type = Board.typeOf(piece);
        boolean capture = Move.isCapture(move, board);
        String dest = squareName(to);
        StringBuilder sb = new StringBuilder();

        if (type == 1) {
            if (capture) sb.append((char)('a' + (from & 7))).append('x');
            sb.append(dest);
            if (Move.isPromotion(move)) sb.append('=').append(promotionLetter(Move.promoType(move)));
        } else {
            sb.append(pieceLetter(type));
            // disambiguation
            MoveGenerator.List legal = new MoveGenerator.List();
            MoveGenerator.generateLegal(board, legal);
            boolean needFile = false, needRank = false, ambiguous = false;
            int sameFile = 0, sameRank = 0, count = 0;
            for (int i = 0; i < legal.size; i++) {
                int m = legal.get(i);
                if (m == move) continue;
                if (Move.to(m) == to && Board.typeOf(board.squares[Move.from(m)]) == type
                        && Board.colorOf(board.squares[Move.from(m)]) == Board.colorOf(piece)) {
                    ambiguous = true;
                    if ((Move.from(m) & 7) == (from & 7)) sameFile++;
                    if ((Move.from(m) >> 3) == (from >> 3)) sameRank++;
                    count++;
                }
            }
            if (ambiguous) {
                if (sameFile == 0) needFile = true;
                else if (sameRank == 0) needRank = true;
                else { needFile = true; needRank = true; }
            }
            if (needFile) sb.append((char)('a' + (from & 7)));
            if (needRank) sb.append((char)('1' + (from >> 3)));
            if (capture) sb.append('x');
            sb.append(dest);
        }

        return finish(board, move, sb.toString());
    }

    private static String finish(Board board, int move, String base) {
        board.makeMove(move);
        String suffix = "";
        MoveGenerator.List legal = new MoveGenerator.List();
        MoveGenerator.generateLegal(board, legal);
        boolean check = board.inCheck(board.sideToMove);
        if (check) suffix = legal.size == 0 ? "#" : "+";
        board.unmakeMove(move);
        return base + suffix;
    }

    public static String squareName(int sq) {
        return "" + (char)('a' + (sq & 7)) + (char)('1' + (sq >> 3));
    }
    private static String pieceLetter(int type) {
        switch (type) {
            case 2: return "N";
            case 3: return "B";
            case 4: return "R";
            case 5: return "Q";
            case 6: return "K";
            default: return "";
        }
    }
    private static String promotionLetter(int type) {
        switch (type) {
            case 2: return "N";
            case 3: return "B";
            case 4: return "R";
            case 5: return "Q";
            default: return "Q";
        }
    }
}
