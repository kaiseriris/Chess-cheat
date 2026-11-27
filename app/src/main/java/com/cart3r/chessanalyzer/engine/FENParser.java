package com.cart3r.chessanalyzer.engine;

public final class FENParser {
    private FENParser() {}

    public static Board parse(String fen) {
        Board b = new Board();
        String[] parts = fen.trim().split("\\s+");
        if (parts.length < 4) throw new IllegalArgumentException("Invalid FEN");

        int rank = 7, file = 0;
        for (char c : parts[0].toCharArray()) {
            if (c == '/') { rank--; file = 0; continue; }
            if (c >= '1' && c <= '8') { file += c - '0'; continue; }
            b.squares[rank * 8 + file] = pieceFromChar(c);
            file++;
        }

        b.sideToMove = parts[1].equals("b") ? Board.BLACK : Board.WHITE;

        int cr = 0;
        if (parts[2].contains("K")) cr |= 1;
        if (parts[2].contains("Q")) cr |= 2;
        if (parts[2].contains("k")) cr |= 4;
        if (parts[2].contains("q")) cr |= 8;
        b.castlingRights = cr;

        if (!parts[3].equals("-")) {
            int epFile = parts[3].charAt(0) - 'a';
            int epRank = parts[3].charAt(1) - '1';
            b.epSquare = epRank * 8 + epFile;
        } else b.epSquare = -1;

        if (parts.length > 4) b.halfmoveClock = Integer.parseInt(parts[4]);
        if (parts.length > 5) b.fullmoveNumber = Integer.parseInt(parts[5]);

        b.recomputeState();
        return b;
    }

    public static String toFen(Board b) {
        StringBuilder sb = new StringBuilder();
        for (int r = 7; r >= 0; r--) {
            int empty = 0;
            for (int f = 0; f < 8; f++) {
                int p = b.squares[r*8 + f];
                if (p == 0) { empty++; continue; }
                if (empty > 0) { sb.append(empty); empty = 0; }
                sb.append(charFromPiece(p));
            }
            if (empty > 0) sb.append(empty);
            if (r > 0) sb.append('/');
        }
        sb.append(' ').append(b.sideToMove == Board.WHITE ? 'w' : 'b');
        sb.append(' ');
        StringBuilder cr = new StringBuilder();
        if ((b.castlingRights & 1) != 0) cr.append('K');
        if ((b.castlingRights & 2) != 0) cr.append('Q');
        if ((b.castlingRights & 4) != 0) cr.append('k');
        if ((b.castlingRights & 8) != 0) cr.append('q');
        sb.append(cr.length() == 0 ? "-" : cr.toString());
        sb.append(' ');
        if (b.epSquare < 0) sb.append('-');
        else {
            sb.append((char)('a' + (b.epSquare & 7)));
            sb.append((char)('1' + (b.epSquare >> 3)));
        }
        sb.append(' ').append(b.halfmoveClock).append(' ').append(b.fullmoveNumber);
        return sb.toString();
    }

    static int pieceFromChar(char c) {
        int t;
        switch (Character.toLowerCase(c)) {
            case 'p': t = 1; break;
            case 'n': t = 2; break;
            case 'b': t = 3; break;
            case 'r': t = 4; break;
            case 'q': t = 5; break;
            case 'k': t = 6; break;
            default: return 0;
        }
        return Character.isUpperCase(c) ? t : -t;
    }

    static char charFromPiece(int p) {
        char c;
        switch (Board.typeOf(p)) {
            case 1: c = 'p'; break;
            case 2: c = 'n'; break;
            case 3: c = 'b'; break;
            case 4: c = 'r'; break;
            case 5: c = 'q'; break;
            case 6: c = 'k'; break;
            default: return '?';
        }
        return p > 0 ? Character.toUpperCase(c) : c;
    }
}
