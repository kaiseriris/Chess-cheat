package com.cart3r.chessanalyzer.engine;

import java.util.Random;

public final class Zobrist {
    // pieceKeys[color][type(1..6)][sq]
    public static final long[][][] pieceKeys = new long[2][7][64];
    public static final long[] castlingKeys = new long[16];
    public static final long[] epKeys = new long[8];
    public static final long sideKey;

    static {
        Random r = new Random(0xC0FFEEL);
        for (int c = 0; c < 2; c++)
            for (int t = 1; t <= 6; t++)
                for (int s = 0; s < 64; s++)
                    pieceKeys[c][t][s] = r.nextLong();
        for (int i = 0; i < 16; i++) castlingKeys[i] = r.nextLong();
        for (int i = 0; i < 8; i++)  epKeys[i] = r.nextLong();
        sideKey = r.nextLong();
    }
    private Zobrist() {}
}
