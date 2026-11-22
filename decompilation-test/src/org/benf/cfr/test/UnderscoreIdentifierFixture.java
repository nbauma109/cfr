package org.benf.cfr.test;

public class UnderscoreIdentifierFixture {
    private int _ = 7;

    public int calculate() {
        int _ = 5;
        return _ + this._;
    }
}
