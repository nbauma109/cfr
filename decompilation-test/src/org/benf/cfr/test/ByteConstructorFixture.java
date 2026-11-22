package org.benf.cfr.test;

public class ByteConstructorFixture {
    public int construct() {
        return new ByteConstructorTarget((byte) -70).value();
    }
}

class ByteConstructorTarget {
    private final byte value;

    ByteConstructorTarget(byte value) {
        this.value = value;
    }

    int value() {
        return value;
    }
}
