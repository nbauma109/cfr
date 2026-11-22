package org.benf.cfr.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
@interface ConstructorMarker {}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@interface ParameterMarker {}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE_USE)
@interface TypeMarker {}

record AnnotatedRecordFixture(int value) {
    @ConstructorMarker
    AnnotatedRecordFixture(@ParameterMarker int value) {
        this.value = value;
    }

    public @TypeMarker int value() {
        return value;
    }
}
