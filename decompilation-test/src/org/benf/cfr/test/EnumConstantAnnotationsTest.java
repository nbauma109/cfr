package org.benf.cfr.test;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EnumConstantAnnotationsTest {
    @Test
    void retainsAnnotationsOnEnumConstants() {
        Path classFile = Paths.get("target", "test-classes", "org", "benf", "cfr", "test",
                "EnumConstantAnnotationFixture.class");
        assertTrue(Files.exists(classFile), "Missing enum class file: " + classFile);

        StringBuilder decompiledSource = new StringBuilder();
        OutputSinkFactory sinkFactory = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> available) {
                return Collections.singletonList(SinkClass.STRING);
            }

            @Override
            public <T> Sink<T> getSink(final SinkType sinkType, SinkClass sinkClass) {
                return value -> {
                    if (sinkType == SinkType.JAVA) {
                        decompiledSource.append(value);
                    }
                };
            }
        };
        CfrDriver driver = new CfrDriver.Builder()
                .withOptions(Collections.singletonMap(OptionsImpl.SHOW_CFR_VERSION.getName(), "false"))
                .withOutputSink(sinkFactory)
                .build();
        driver.analyse(Collections.singletonList(classFile.toAbsolutePath().toString()));

        String source = decompiledSource.toString();
        assertTrue(Pattern.compile("@(?:[\\w$.]+\\.)?FieldMarker\\s+FIRST\\b")
                .matcher(source).find(), source);
        assertTrue(Pattern.compile("@(?:[\\w$.]+\\.)?FieldMarker\\(value=\\\"second\\\"\\)\\s+SECOND\\b")
                .matcher(source).find(), source);
    }
}
