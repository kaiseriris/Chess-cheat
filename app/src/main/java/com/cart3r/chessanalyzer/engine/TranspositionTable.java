package com.cart3r.chessanalyzer.engine;

public final class TranspositionTable {
    public static final int EXACT = 0, LOWER = 1, UPPER = 2;
    public static final class Entry {
        long key;
        int depth;
        int score;
        int move;
        int flag;
        int age;
    }
    public final Entry[] table;
    private final int mask;

    public TranspositionTable(int logSize) {
        int size = 1 << logSize;
        mask = size - 1;
        table = new Entry[size];
        for (int i = 0; i < size; i++) table[i] = new Entry();
    }

    public void newSearch() {
        for (Entry e : table) e.age++;
    }

    public Entry probe(long key) {
        Entry e = table[(int)(key & mask)];
        return (e.key == key && e.depth >= 0) ? e : null;
    }

    public void store(long key, int depth, int score, int move, int flag) {
        int idx = (int)(key & mask);
        Entry e = table[idx];
        if (e.key == key && e.depth > depth) return;
        e.key = key;
        e.depth = depth;
        e.score = score;
        e.move = move;
        e.flag = flag;
        e.age = 0;
    }

    public void clear() {
        for (Entry e : table) { e.key = 0; e.depth = -1; e.score = 0; e.move = 0; e.flag = 0; e.age = 0; }
    }
}
