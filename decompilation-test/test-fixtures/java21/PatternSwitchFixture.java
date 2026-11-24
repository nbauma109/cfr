package org.benf.cfr.test;

public class PatternSwitchFixture {
    public static int classify(Object value) {
        return switch (value) {
            case String text -> text.length();
            case Integer number -> number.intValue();
            case null -> -1;
            default -> 0;
        };
    }

    public static String inspect(Object value) {
        return switch (value) {
            case String text when text.length() > 4 -> "long " + text;
            case String text -> "short " + text;
            case Integer number -> "number " + number;
            default -> "other";
        };
    }
}
