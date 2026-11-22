package org.benf.cfr.test.fixtures;

public final class EcjArrayComparisonFixture {
    private EcjArrayComparisonFixture() {
    }

    public static boolean cmp(int[] first, int[] second) {
        if (first == null) {
            return second == null;
        } else if (second == null) {
            return false;
        } else {
            int index = first.length;
            if (index != second.length) {
                return false;
            } else {
                while (index-- > 0) {
                    if (first[index] != second[index]) {
                        return false;
                    }
                }
                return true;
            }
        }
    }
}
