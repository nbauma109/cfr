package org.benf.cfr.test;

public class BoundedGenericFieldFixture<T extends Number> {
    public T a;

    public void write(T value) {
        this.a = value;
    }

    public T read() {
        return this.a;
    }
}
