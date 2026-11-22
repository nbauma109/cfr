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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeaderOutputTest {
    @Test
    void preservesHeaderByDefault() {
        String source = decompile(Collections.<String, String>emptyMap());
        assertTrue(source.startsWith("/*"), source);
        assertTrue(source.contains("Decompiled with CFR"), source);
    }

    @Test
    void suppressesHeaderWhilePreservingPackage() {
        Map<String, String> options = new HashMap<>();
        options.put(OptionsImpl.SHOW_CFR_HEADER.getName(), "false");
        String source = decompile(options);
        assertTrue(source.startsWith("package org.benf.cfr.test;"), source);
        assertFalse(source.contains("Decompiled with CFR"), source);
    }

    private static String decompile(Map<String, String> options) {
        Path classFile = Paths.get("target", "test-classes", "org", "benf", "cfr",
                "test", "HeaderOutputFixture.class");
        assertTrue(Files.exists(classFile), "Missing fixture: " + classFile);

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
                .withOptions(options)
                .withOutputSink(sinkFactory)
                .build();
        driver.analyse(Collections.singletonList(classFile.toAbsolutePath().toString()));
        return output.toString();
    }
}
