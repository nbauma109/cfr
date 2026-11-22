package org.benf.cfr.test;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.eclipse.jdt.core.compiler.batch.BatchCompiler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class EcjArrayComparisonTest {
    private static final String CLASS_NAME = "org.benf.cfr.test.fixtures.EcjArrayComparisonFixture";

    @Test
    void preservesArrayComparisonCompiledByEclipse(@TempDir Path temporaryDirectory) throws Exception {
        Path source = Paths.get("decompilation-test", "fixtures", "ecj",
                "EcjArrayComparisonFixture.java").toAbsolutePath();
        Path original = temporaryDirectory.resolve("original");
        Files.createDirectories(original);

        StringWriter compilerMessages = new StringWriter();
        boolean compiled = BatchCompiler.compile(
                "-1.8 -g:none -d \"" + original + "\" \"" + source + "\"",
                new PrintWriter(compilerMessages), new PrintWriter(compilerMessages), null);
        assertTrue(compiled, compilerMessages.toString());

        Path classFile = original.resolve(Paths.get("org", "benf", "cfr", "test", "fixtures",
                "EcjArrayComparisonFixture.class"));
        StringBuilder decompiled = new StringBuilder();
        CfrDriver driver = new CfrDriver.Builder()
                .withOptions(Collections.singletonMap(OptionsImpl.SHOW_CFR_VERSION.getName(), "false"))
                .withOutputSink(sourceSink(decompiled))
                .build();
        driver.analyse(Collections.singletonList(classFile.toString()));

        String generatedSource = decompiled.toString();
        assertFalse(generatedSource.contains("Unable to fully structure code"), generatedSource);
        assertFalse(generatedSource.contains("** GOTO"), generatedSource);

        Path regeneratedSource = temporaryDirectory.resolve("generated").resolve(Paths.get("org",
                "benf", "cfr", "test", "fixtures", "EcjArrayComparisonFixture.java"));
        Files.createDirectories(regeneratedSource.getParent());
        Files.write(regeneratedSource, generatedSource.getBytes(StandardCharsets.UTF_8));

        Path output = temporaryDirectory.resolve("recompiled");
        Files.createDirectories(output);
        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(javaCompiler);
        ByteArrayOutputStream errors = new ByteArrayOutputStream();
        int exitCode = javaCompiler.run(null, errors, errors, "-d", output.toString(),
                regeneratedSource.toString());
        assertEquals(0, exitCode, new String(errors.toByteArray(), StandardCharsets.UTF_8));

        assertEquivalentBehavior(original, output);
    }

    private static OutputSinkFactory sourceSink(final StringBuilder output) {
        return new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType type, Collection<SinkClass> available) {
                return Collections.singletonList(SinkClass.STRING);
            }

            @Override
            public <T> Sink<T> getSink(final SinkType type, SinkClass sinkClass) {
                return value -> {
                    if (type == SinkType.JAVA) {
                        output.append(value);
                    }
                };
            }
        };
    }

    private static void assertEquivalentBehavior(Path original, Path recompiled) throws Exception {
        URL[] originalClasspath = {original.toUri().toURL()};
        URL[] regeneratedClasspath = {recompiled.toUri().toURL()};
        try (URLClassLoader originalLoader = new URLClassLoader(originalClasspath, null);
             URLClassLoader regeneratedLoader = new URLClassLoader(regeneratedClasspath, null)) {
            Class<?> originalClass = originalLoader.loadClass(CLASS_NAME);
            Class<?> regeneratedClass = regeneratedLoader.loadClass(CLASS_NAME);
            int[][] samples = {null, {}, {1}, {1, 2}, {1, 3}, {1, 2, 3}};
            for (int[] first : samples) {
                for (int[] second : samples) {
                    Object[] inputs = {first, second};
                    Object expected = originalClass.getMethod("cmp", int[].class, int[].class)
                            .invoke(null, inputs);
                    Object actual = regeneratedClass.getMethod("cmp", int[].class, int[].class)
                            .invoke(null, inputs);
                    assertEquals(expected, actual);
                }
            }
        }
    }
}
