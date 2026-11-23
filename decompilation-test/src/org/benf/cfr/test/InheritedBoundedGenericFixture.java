package org.benf.cfr.test;

class InheritedBoundedGenericParent<T extends Number> {
    public T a;
}

public class InheritedBoundedGenericFixture extends InheritedBoundedGenericParent<Integer> {
    public Number readNumber() {
        Number number = this.a;
        System.out.println(number);
        return number;
    }
}
