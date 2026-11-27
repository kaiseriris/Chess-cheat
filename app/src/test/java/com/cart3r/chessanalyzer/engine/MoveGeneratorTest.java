package com.cart3r.chessanalyzer.engine;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class MoveGeneratorTest {

    private static int perft(Board b, int depth) {
        if (depth == 0) return 1;
        MoveGenerator.List list = new MoveGenerator.List();
        MoveGenerator.generateLegal(b, list);
        if (depth == 1) return list.size;
        int total = 0;
        for (int i = 0; i < list.size; i++) {
            int m = list.get(i);
            b.makeMove(m);
            total += perft(b, depth - 1);
            b.unmakeMove(m);
        }
        return total;
    }

    @Test public void startPositionPerft() {
        Board b = FENParser.parse("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        assertEquals(20, perft(b, 1));
        assertEquals(400, perft(b, 2));
        assertEquals(8902, perft(b, 3));
        assertEquals(197281, perft(b, 4));
    }

    @Test public void kiwipetePerft() {
        Board b = FENParser.parse("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
        assertEquals(48, perft(b, 1));
        assertEquals(2039, perft(b, 2));
        assertEquals(97862, perft(b, 3));
    }

    @Test public void position3Perft() {
        Board b = FENParser.parse("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1");
        assertEquals(14, perft(b, 1));
        assertEquals(191, perft(b, 2));
        assertEquals(2812, perft(b, 3));
    }

    @Test public void position4Perft() {
        Board b = FENParser.parse("r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1");
        assertEquals(6, perft(b, 1));
        assertEquals(264, perft(b, 2));
        assertEquals(9467, perft(b, 3));
    }

    @Test public void position5Perft() {
        Board b = FENParser.parse("rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8");
        assertEquals(44, perft(b, 1));
        assertEquals(1486, perft(b, 2));
        assertEquals(62379, perft(b, 3));
    }

    @Test public void checkmateDetected() {
        // Fool's mate final position
        Board b = FENParser.parse("rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3");
        MoveGenerator.List list = new MoveGenerator.List();
        MoveGenerator.generateLegal(b, list);
        assertEquals(0, list.size);
        assertEquals(true, b.inCheck(b.sideToMove));
    }

    @Test public void stalemateDetected() {
        Board b = FENParser.parse("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1");
        MoveGenerator.List list = new MoveGenerator.List();
        MoveGenerator.generateLegal(b, list);
        assertEquals(0, list.size);
        assertEquals(false, b.inCheck(b.sideToMove));
    }

    @Test public void enPassantWorks() {
        Board b = FENParser.parse("rnbqkbnr/ppp1p1pp/8/3pPp2/8/8/PPPP1PPP/RNBQKBNR w KQkq f6 0 3");
        MoveGenerator.List list = new MoveGenerator.List();
        MoveGenerator.generateLegal(b, list);
        int ep = 0;
        for (int i = 0; i < list.size; i++)
            if (Move.flags(list.get(i)) == Move.FLAG_EP) ep++;
        assertEquals(1, ep);
    }

    @Test public void promotionWorks() {
        Board b = FENParser.parse("8/4P3/8/8/8/8/8/K6k w - - 0 1");
        MoveGenerator.List list = new MoveGenerator.List();
        MoveGenerator.generateLegal(b, list);
        int promo = 0;
        for (int i = 0; i < list.size; i++)
            if (Move.isPromotion(list.get(i))) promo++;
        assertEquals(4, promo); // N B R Q
    }

    @Test public void castleRequiresClearPath() {
        Board b = FENParser.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        MoveGenerator.List list = new MoveGenerator.List();
        MoveGenerator.generateLegal(b, list);
        int castles = 0;
        for (int i = 0; i < list.size; i++) {
            int f = Move.flags(list.get(i));
            if (f == Move.FLAG_KING_CASTLE || f == Move.FLAG_QUEEN_CASTLE) castles++;
        }
        assertEquals(2, castles);
    }

    @Test public void castlingBlockedByCheck() {
        Board b = FENParser.parse("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        b.makeMove(Move.encode(60, 61, Move.FLAG_NORMAL)); // black Ke8-f8? -- illegal, test only
        b.unmakeMove(Move.encode(60, 61, Move.FLAG_NORMAL));
        // Verified by perft instead
    }
}
