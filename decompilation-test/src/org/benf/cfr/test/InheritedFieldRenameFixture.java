package org.benf.cfr.test;

public class InheritedFieldRenameFixture extends InheritedFieldRenameParent {
    public InheritedFieldRenameFixture(String value) {
        this.a = value;
    }

    public String read() {
        return this.a;
    }
}

class InheritedFieldRenameParent {
    protected String a;

    public int length() {
        return this.a.length();
    }
}
