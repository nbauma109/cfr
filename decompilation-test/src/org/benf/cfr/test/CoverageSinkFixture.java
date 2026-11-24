package org.benf.cfr.test;

public class CoverageSinkFixture {
    private int count = 7;

    public int increment(int value) {
        count += value;
        return count;
    }

    public int twice(int value) {
        return increment(value) + increment(value);
    }
}
