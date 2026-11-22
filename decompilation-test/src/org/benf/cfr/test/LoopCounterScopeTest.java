package org.benf.cfr.test;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LoopCounterScopeTest {
    @Test
    void preservesLoopCounterUsedAfterForLoop(@TempDir Path temporaryDirectory) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);

        Path fixtureSource = Paths.get("decompilation-test", "src", "org", "benf", "cfr", "test",
                "LoopCounterOutlivesForFixture.java");
        Path originalDirectory = temporaryDirectory.resolve("original");
        compile(compiler, fixtureSource, originalDirectory);

        Path classFile = originalDirectory.resolve(Paths.get("org", "benf", "cfr", "test",
                "LoopCounterOutlivesForFixture.class"));
        StringBuilder decompiledSource = new StringBuilder();
        CfrDriver driver = new CfrDriver.Builder()
                .withOptions(Collections.singletonMap(OptionsImpl.SHOW_CFR_VERSION.getName(), "false"))
                .withOutputSink(sourceSink(decompiledSource))
                .build();
        driver.analyse(Collections.singletonList(classFile.toString()));
        assertFalse(decompiledSource.toString().trim().isEmpty());

        Path generatedSource = temporaryDirectory.resolve("generated").resolve(Paths.get("org",
                "benf", "cfr", "test", "LoopCounterOutlivesForFixture.java"));
        Files.createDirectories(generatedSource.getParent());
        Files.write(generatedSource, decompiledSource.toString().getBytes(StandardCharsets.UTF_8));

        Path roundTripDirectory = temporaryDirectory.resolve("roundtrip");
        compile(compiler, generatedSource, roundTripDirectory);
        assertRoundTripBehavior(roundTripDirectory);
    }

    private static void compile(JavaCompiler compiler, Path source, Path output) throws Exception {
        Files.createDirectories(output);
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        int result = compiler.run(null, diagnostics, diagnostics, "-g:none", "-d",
                output.toString(), source.toString());
        assertEquals(0, result, new String(diagnostics.toByteArray(), StandardCharsets.UTF_8));
    }

    private static OutputSinkFactory sourceSink(final StringBuilder source) {
        return new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> available) {
                return Collections.singletonList(SinkClass.STRING);
            }

            @Override
            public <T> Sink<T> getSink(final SinkType sinkType, SinkClass sinkClass) {
                return value -> {
                    if (sinkType == SinkType.JAVA) {
                        source.append(value);
                    }
                };
            }
        };
    }

    private static void assertRoundTripBehavior(Path compiledDirectory) throws Exception {
        URL[] classpath = {compiledDirectory.toUri().toURL()};
        try (URLClassLoader classLoader = new URLClassLoader(classpath, null)) {
            Class<?> fixtureClass = classLoader.loadClass(LoopCounterOutlivesForFixture.class.getName());
            Object fixture = fixtureClass.getDeclaredConstructor().newInstance();
            fixtureClass.getMethod("markTerminalSlot", int.class).invoke(null, 2);
            fixtureClass.getMethod("markFirstVacant", int.class).invoke(fixture, 9);

            assertEquals(9, fixtureClass.getMethod("getRecordedValue", int.class).invoke(null, 2));
            assertEquals(73, fixtureClass.getMethod("getFieldValue").invoke(fixture));
        }
    }
}
