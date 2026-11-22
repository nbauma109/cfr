package org.benf.cfr.test;

public class WrapperCastFixture<M extends String> extends WrapperCastMiddle {
    public WrapperCastFixture() {
        super(new WrapperCastChild((short) -55, 96L), (short) -48);
    }
}

class WrapperCastParent {
    WrapperCastParent(long value, Short boxedValue) {
    }
}

class WrapperCastChild extends WrapperCastParent {
    WrapperCastChild(Short boxedValue, long value) {
        super(-38L, (short) -37);
    }
}

class WrapperCastMiddle extends WrapperCastParent {
    WrapperCastMiddle(WrapperCastChild child, Short boxedValue) {
        super(-38L, (short) -37);
    }
}
