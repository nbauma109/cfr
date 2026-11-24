package org.benf.cfr.test;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.util.getopt.OptionsImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatternSwitchCoverageTest {
    @Test
    void decompilesTypeSwitchAndGuards(@TempDir Path temporaryDirectory) throws Exception {
        Path classFile = Paths.get("decompilation-test", "test-fixtures", "java21",
                "PatternSwitchFixture.class");
        assertTrue(Files.isRegularFile(classFile), "Missing precompiled fixture: " + classFile);

        StringBuilder output = new StringBuilder();
        OutputSinkFactory sinks = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType type, Collection<SinkClass> available) {
                return Collections.singletonList(SinkClass.STRING);
            }

            @Override
            public <T> Sink<T> getSink(SinkType type, SinkClass sinkClass) {
                return value -> {
                    if (type == SinkType.JAVA) {
                        output.append(value);
                    }
                };
            }
        };
        CfrDriver driver = new CfrDriver.Builder()
                .withOptions(Collections.singletonMap(OptionsImpl.SHOW_CFR_VERSION.getName(), "false"))
                .withOutputSink(sinks)
                .build();
        driver.analyse(Collections.singletonList(classFile.toAbsolutePath().toString()));

        String javaSource = output.toString();
        assertTrue(javaSource.contains("switch ("), javaSource);
        assertTrue(javaSource.contains("case String "), javaSource);
        assertTrue(javaSource.contains("case Integer "), javaSource);
        assertTrue(javaSource.contains("case null -> -1"), javaSource);
        assertTrue(Pattern.compile("case\\s+String\\s+\\w+\\s+when\\s+")
                .matcher(javaSource).find(), javaSource);
        assertFalse(javaSource.contains("** GOTO"), javaSource);
        assertFalse(javaSource.contains("Decompilation failed"), javaSource);

        if (getJavaVersion() >= 21) {
            recompileAndVerifyBehavior(javaSource, temporaryDirectory);
        }
    }

    private static int getJavaVersion() {
        String version = System.getProperty("java.specification.version");
        if (version.startsWith("1.")) {
            return Integer.parseInt(version.substring(2));
        }
        return Integer.parseInt(version);
    }

    private static void recompileAndVerifyBehavior(String source, Path temporaryDirectory) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "A Java compiler is required for the runtime verification");
        Path sourceFile = temporaryDirectory.resolve(Paths.get("org", "benf", "cfr",
                "test", "PatternSwitchFixture.java"));
        Files.createDirectories(sourceFile.getParent());
        Files.write(sourceFile, source.getBytes(StandardCharsets.UTF_8));

        Path classes = temporaryDirectory.resolve("classes");
        Files.createDirectories(classes);
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        int exitCode = compiler.run(null, diagnostics, diagnostics, "--release", "21",
                "-d", classes.toString(), sourceFile.toString());
        assertEquals(0, exitCode, new String(diagnostics.toByteArray(), StandardCharsets.UTF_8));

        try (URLClassLoader loader = new URLClassLoader(new URL[]{classes.toUri().toURL()}, null)) {
            Class<?> result = Class.forName("org.benf.cfr.test.PatternSwitchFixture", true, loader);
            Method classify = result.getMethod("classify", Object.class);
            assertEquals(4, classify.invoke(null, "test"));
            assertEquals(5, classify.invoke(null, Integer.valueOf(5)));
            assertEquals(-1, classify.invoke(null, new Object[]{null}));
            assertEquals(0, classify.invoke(null, new Object()));

            Method inspect = result.getMethod("inspect", Object.class);
            assertEquals("long hello", inspect.invoke(null, "hello"));
            assertEquals("short hi", inspect.invoke(null, "hi"));
            assertEquals("number 3", inspect.invoke(null, Integer.valueOf(3)));
            assertEquals("other", inspect.invoke(null, new Object()));
            InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                    () -> inspect.invoke(null, new Object[]{null}));
            assertTrue(exception.getCause() instanceof NullPointerException);
        }
    }
}
