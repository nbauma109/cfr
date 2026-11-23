package org.benf.cfr.test;

interface InheritedInterfaceFieldValues {
    int value = Integer.parseInt("2");
}

class InheritedInterfaceFieldParent {
    private static boolean value;
}

public class InheritedInterfaceFieldFixture extends InheritedInterfaceFieldParent
        implements InheritedInterfaceFieldValues {
    public boolean isZero() {
        return InheritedInterfaceFieldFixture.value == 0;
    }
}
