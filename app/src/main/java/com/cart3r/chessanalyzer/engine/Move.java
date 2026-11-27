package com.cart3r.chessanalyzer.engine;

/** Move encoding: int = (flags<<12) | (from<<6) | to. */
public final class Move {
    public static final int FLAG_NORMAL        = 0;
    public static final int FLAG_DOUBLE_PUSH   = 1;
    public static final int FLAG_KING_CASTLE   = 2;
    public static final int FLAG_QUEEN_CASTLE  = 3;
    public static final int FLAG_EP            = 4;
    public static final int FLAG_PROMO_N       = 5;
    public static final int FLAG_PROMO_B       = 6;
    public static final int FLAG_PROMO_R       = 7;
    public static final int FLAG_PROMO_Q       = 8;
    public static final int FLAG_PROMO_CAP_N   = 9;
    public static final int FLAG_PROMO_CAP_B   = 10;
    public static final int FLAG_PROMO_CAP_R   = 11;
    public static final int FLAG_PROMO_CAP_Q   = 12;

    private Move() {}

    public static int encode(int from, int to, int flags) {
        return from | (to << 6) | (flags << 12);
    }
    public static int from(int m)  { return m & 63; }
    public static int to(int m)    { return (m >> 6) & 63; }
    public static int flags(int m) { return (m >> 12) & 15; }

    public static boolean isPromotion(int m) {
        int f = flags(m);
        return f >= FLAG_PROMO_N && f <= FLAG_PROMO_CAP_Q;
    }
    public static boolean isCapture(int m, Board b) {
        int f = flags(m);
        if (f == FLAG_EP) return true;
        if (f >= FLAG_PROMO_CAP_N && f <= FLAG_PROMO_CAP_Q) return true;
        return b.squares[to(m)] != 0;
    }
    public static int promoType(int m) {
        switch (flags(m)) {
            case FLAG_PROMO_N: case FLAG_PROMO_CAP_N: return 2;
            case FLAG_PROMO_B: case FLAG_PROMO_CAP_B: return 3;
            case FLAG_PROMO_R: case FLAG_PROMO_CAP_R: return 4;
            case FLAG_PROMO_Q: case FLAG_PROMO_CAP_Q: return 5;
            default: return 0;
        }
    }
}
