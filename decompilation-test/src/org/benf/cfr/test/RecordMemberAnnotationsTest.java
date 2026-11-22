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

class RecordMemberAnnotationsTest {
    @Test
    void preservesConstructorParameterAndAccessorAnnotations() {
        Path classFile = Paths.get("decompilation-test", "test-fixtures", "records",
                "AnnotatedRecordFixture.class");
        assertTrue(Files.isRegularFile(classFile), "Missing compiled record fixture: " + classFile);

        StringBuilder source = new StringBuilder();
        OutputSinkFactory sinks = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType type, Collection<SinkClass> available) {
                return Collections.singletonList(SinkClass.STRING);
            }

            @Override
            public <T> Sink<T> getSink(final SinkType type, SinkClass sinkClass) {
                return value -> {
                    if (type == SinkType.JAVA) {
                        source.append(value);
                    }
                };
            }
        };
        CfrDriver driver = new CfrDriver.Builder()
                .withOptions(Collections.singletonMap(OptionsImpl.SHOW_CFR_VERSION.getName(), "false"))
                .withOutputSink(sinks)
                .build();
        driver.analyse(Collections.singletonList(classFile.toAbsolutePath().toString()));

        String output = source.toString();
        assertTrue(Pattern.compile("@(?:[\\w$.]+\\.)?ConstructorMarker\\s+AnnotatedRecordFixture\\s*\\(")
                .matcher(output).find(), output);
        assertTrue(Pattern.compile("AnnotatedRecordFixture\\s*\\(\\s*@(\\w+\\.)?ParameterMarker\\s+int\\s+value\\s*\\)")
                .matcher(output).find(), output);
        assertTrue(Pattern.compile("@(?:[\\w$.]+\\.)?TypeMarker\\s+int\\s+value\\s*\\(")
                .matcher(output).find(), output);
    }
}
