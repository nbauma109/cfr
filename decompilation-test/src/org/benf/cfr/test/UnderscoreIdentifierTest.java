package org.benf.cfr.test;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnderscoreIdentifierTest {
    @Test
    void renamesUnderscoreFieldAndLocalVariable(@TempDir Path temporaryDirectory) throws Exception {
        Path classFile = Paths.get("target", "test-classes", "org", "benf", "cfr", "test",
                "UnderscoreIdentifierFixture.class");
        assertTrue(Files.isRegularFile(classFile), "Missing fixture: " + classFile);

        StringBuilder decompiledJava = new StringBuilder();
        OutputSinkFactory sinks = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType type, Collection<SinkClass> available) {
                return Collections.singletonList(SinkClass.STRING);
            }

            @Override
            public <T> Sink<T> getSink(final SinkType type, SinkClass sinkClass) {
                return value -> {
                    if (type == SinkType.JAVA) {
                        decompiledJava.append(value);
                    }
                };
            }
        };
        Map<String, String> options = new HashMap<>();
        options.put(OptionsImpl.SHOW_CFR_VERSION.getName(), "false");
        options.put(OptionsImpl.USE_NAME_TABLE.getName(), "true");
        options.put(OptionsImpl.RENAME_ILLEGAL_IDENTS.getName(), "true");
        CfrDriver driver = new CfrDriver.Builder().withOptions(options).withOutputSink(sinks).build();
        driver.analyse(Collections.singletonList(classFile.toAbsolutePath().toString()));

        String output = decompiledJava.toString();
        assertTrue(output.contains("cfr_renamed_"), output);
        assertFalse(Pattern.compile("(?<![A-Za-z0-9_$])_(?![A-Za-z0-9_$])")
                .matcher(output).find(), output);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "The regression test requires a Java compiler");
        Path generatedSource = temporaryDirectory.resolve(Paths.get("org", "benf", "cfr",
                "test", "UnderscoreIdentifierFixture.java"));
        Files.createDirectories(generatedSource.getParent());
        Files.write(generatedSource, output.getBytes(StandardCharsets.UTF_8));
        Path compiledDirectory = temporaryDirectory.resolve("compiled");
        Files.createDirectories(compiledDirectory);
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        int result = compiler.run(null, diagnostics, diagnostics, "-d", compiledDirectory.toString(),
                generatedSource.toString());
        assertEquals(0, result, new String(diagnostics.toByteArray(), StandardCharsets.UTF_8));
    }
}
