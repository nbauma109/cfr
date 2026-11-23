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

class RecordVarargsTest {
    @Test
    void emitsVarargsForRecordWithVarargsCanonicalConstructor() {
        String source = decompile("RecordVarargsFixture");
        assertTrue(Pattern.compile("record\\s+RecordVarargsFixture\\s*\\(\\s*(?:java\\.lang\\.)?String\\s*\\.\\.\\.\\s*values")
                .matcher(source).find(), source);
    }

    @Test
    void preservesArraySyntaxForRecordWithoutVarargsConstructor() {
        String source = decompile("RecordArrayFixture");
        assertTrue(Pattern.compile("record\\s+RecordArrayFixture\\s*\\(\\s*(?:java\\.lang\\.)?String\\[\\]\\s+values")
                .matcher(source).find(), source);
    }

    @Test
    void emitsVarargsForGenericRecord() {
        String source = decompile("RecordSignatureVarargsFixture");
        assertTrue(Pattern.compile("record\\s+RecordSignatureVarargsFixture\\s*<T>\\s*\\(\\s*T\\s*\\.\\.\\.\\s*values")
                .matcher(source).find(), source);
    }

    private static String decompile(String simpleClassName) {
        Path classFile = Paths.get("decompilation-test", "fixtures", "record-varargs",
                simpleClassName + ".class");
        assertTrue(Files.exists(classFile), "Missing record fixture: " + classFile);

        StringBuilder output = new StringBuilder();
        OutputSinkFactory sinkFactory = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> available) {
                return Collections.singletonList(SinkClass.STRING);
            }

            @Override
            public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
                return value -> {
                    if (sinkType == SinkType.JAVA) {
                        output.append(value);
                    }
                };
            }
        };
        CfrDriver driver = new CfrDriver.Builder()
                .withOptions(Collections.singletonMap(OptionsImpl.SHOW_CFR_VERSION.getName(), "false"))
                .withOutputSink(sinkFactory)
                .build();
        driver.analyse(Collections.singletonList(classFile.toAbsolutePath().toString()));
        return output.toString();
    }
}
