package org.benf.cfr.test;

import java.util.List;
import java.util.function.Consumer;

public class RawEnhancedForFieldFixture {
    public List<String> strings;
    public List<String> alternatives;
    public boolean choose;

    public void consume() {
        List<String> values = strings;
        for (String value : values) {
            System.out.println(value);
        }
    }

    public void consumeConditional() {
        for (String value : choose ? strings : alternatives) {
            System.out.println(value);
        }
    }

    public void consumeWildcardCallback(List values, Consumer<?> callback) {
        values.forEach(callback);
    }

    public void consumeSuperWildcardCallback(List values, Consumer<? super String> callback) {
        values.forEach(callback);
    }

    public void consumeFunctionalCalls() {
        List<String> values = strings;
        values.forEach(String::trim);
        values.forEach(value -> System.out.println(value.length()));
    }
}
