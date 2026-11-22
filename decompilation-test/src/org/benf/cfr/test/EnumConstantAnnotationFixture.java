package org.benf.cfr.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public enum EnumConstantAnnotationFixture {
    @FieldMarker FIRST,
    @FieldMarker("second") SECOND;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface FieldMarker {
        String value() default "";
    }
}
