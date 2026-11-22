package org.benf.cfr.test;

public class LoopCounterOutlivesForFixture {
    private static int i = 73;
    private static final int[] flags = new int[8];
    private static final int[] marks = new int[8];

    public void markFirstVacant(int value) {
        int cursor = 0;
        for (; cursor < flags.length && flags[cursor] != -1; ++cursor) {
        }
        marks[cursor] = value;
    }

    public static void markTerminalSlot(int slot) {
        flags[slot] = -1;
    }

    public static int getRecordedValue(int slot) {
        return marks[slot];
    }

    public int getFieldValue() {
        return i;
    }
}
