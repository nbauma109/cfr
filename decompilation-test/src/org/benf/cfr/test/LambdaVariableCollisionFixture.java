package org.benf.cfr.test;

import java.util.function.Supplier;

public class LambdaVariableCollisionFixture {
    public String evaluate() {
        Character outer = 'a';
        Supplier<Character> supplier = () -> {
            Character inner = 'b';
            return inner;
        };
        return outer.toString() + supplier.get();
    }
}
