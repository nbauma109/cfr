package org.benf.cfr.test;

import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InheritedFieldRenameTest {
    @Test
    void renamedSuperclassFieldRemainsAccessible(@TempDir Path temporaryDirectory) throws Exception {
        Path classes = Paths.get("target", "test-classes", "org", "benf", "cfr", "test");
        Path childClass = classes.resolve("InheritedFieldRenameFixture.class");
        Path parentClass = classes.resolve("InheritedFieldRenameParent.class");
        assertTrue(Files.isRegularFile(childClass), "Missing child fixture: " + childClass);
        assertTrue(Files.isRegularFile(parentClass), "Missing parent fixture: " + parentClass);

        Map<String, String> generatedSources = new HashMap<>();
        OutputSinkFactory outputSinks = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType type, Collection<SinkClass> available) {
                if (type == SinkType.JAVA && available.contains(SinkClass.DECOMPILED)) {
                    return Collections.singletonList(SinkClass.DECOMPILED);
                }
                return Collections.singletonList(SinkClass.STRING);
            }

            @Override
            public <T> Sink<T> getSink(final SinkType type, SinkClass sinkClass) {
                return value -> {
                    if (type == SinkType.JAVA && value instanceof SinkReturns.Decompiled) {
                        SinkReturns.Decompiled source = (SinkReturns.Decompiled) value;
                        generatedSources.put(source.getClassName(), source.getJava());
                    }
                };
            }
        };
        Map<String, String> options = new HashMap<>();
        options.put(OptionsImpl.SHOW_CFR_VERSION.getName(), "false");
        options.put(OptionsImpl.RENAME_SMALL_MEMBERS.getName(), "1");
        CfrDriver driver = new CfrDriver.Builder()
                .withOptions(options)
                .withOutputSink(outputSinks)
                .build();
        driver.analyse(Arrays.asList(childClass.toAbsolutePath().toString(),
                parentClass.toAbsolutePath().toString()));

        String parent = generatedSources.get("InheritedFieldRenameParent");
        String child = generatedSources.get("InheritedFieldRenameFixture");
        assertNotNull(parent);
        assertNotNull(child);
        assertTrue(parent.contains("var_java_lang_String_a"), parent);
        assertTrue(child.contains("this.var_java_lang_String_a"), child);

        Path outputDirectory = temporaryDirectory.resolve("source");
        Files.createDirectories(outputDirectory);
        Path childSource = outputDirectory.resolve("InheritedFieldRenameFixture.java");
        Path parentSource = outputDirectory.resolve("InheritedFieldRenameParent.java");
        Files.write(childSource, child.getBytes(StandardCharsets.UTF_8));
        Files.write(parentSource, parent.getBytes(StandardCharsets.UTF_8));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);
        Path compiledDirectory = temporaryDirectory.resolve("compiled");
        Files.createDirectories(compiledDirectory);
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        int result = compiler.run(null, diagnostics, diagnostics, "-d", compiledDirectory.toString(),
                parentSource.toString(), childSource.toString());

        assertEquals(0, result, new String(diagnostics.toByteArray(), StandardCharsets.UTF_8));
    }
    @Test
    void resolvesMatchingInterfaceFieldBeforeSuperclass(@TempDir Path temporaryDirectory) throws Exception {
        Path classes = Paths.get("target", "test-classes", "org", "benf", "cfr", "test");
        Path childClass = classes.resolve("InheritedInterfaceFieldFixture.class");
        Path parentClass = classes.resolve("InheritedInterfaceFieldParent.class");
        Path interfaceClass = classes.resolve("InheritedInterfaceFieldValues.class");
        assertTrue(Files.isRegularFile(childClass), "Missing fixture: " + childClass);
        assertTrue(Files.isRegularFile(parentClass), "Missing fixture: " + parentClass);
        assertTrue(Files.isRegularFile(interfaceClass), "Missing fixture: " + interfaceClass);

        Map<String, String> generatedSources = new HashMap<>();
        OutputSinkFactory outputSinks = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType type, Collection<SinkClass> available) {
                if (type == SinkType.JAVA && available.contains(SinkClass.DECOMPILED)) {
                    return Collections.singletonList(SinkClass.DECOMPILED);
                }
                return Collections.singletonList(SinkClass.STRING);
            }

            @Override
            public <T> Sink<T> getSink(final SinkType type, SinkClass sinkClass) {
                return value -> {
                    if (type == SinkType.JAVA && value instanceof SinkReturns.Decompiled) {
                        SinkReturns.Decompiled source = (SinkReturns.Decompiled) value;
                        generatedSources.put(source.getClassName(), source.getJava());
                    }
                };
            }
        };
        CfrDriver driver = new CfrDriver.Builder()
                .withOptions(Collections.singletonMap(OptionsImpl.SHOW_CFR_VERSION.getName(), "false"))
                .withOutputSink(outputSinks)
                .build();
        driver.analyse(Arrays.asList(childClass.toAbsolutePath().toString(),
                interfaceClass.toAbsolutePath().toString(), parentClass.toAbsolutePath().toString()));

        String child = generatedSources.get("InheritedInterfaceFieldFixture");
        assertNotNull(child);
        Path generatedFile = temporaryDirectory.resolve("InheritedInterfaceFieldFixture.java");
        Files.write(generatedFile, child.getBytes(StandardCharsets.UTF_8));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);
        Path compiledDirectory = temporaryDirectory.resolve("compiled");
        Files.createDirectories(compiledDirectory);
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        int result = compiler.run(null, diagnostics, diagnostics, "-classpath",
                Paths.get("target", "test-classes").toAbsolutePath().toString(),
                "-d", compiledDirectory.toString(), generatedFile.toString());

        assertEquals(0, result, new String(diagnostics.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void resolvesBoundedGenericFieldByDescriptor(@TempDir Path temporaryDirectory) throws Exception {
        Path classFile = Paths.get("target", "test-classes", "org", "benf", "cfr", "test",
                "BoundedGenericFieldFixture.class");
        assertTrue(Files.isRegularFile(classFile), "Missing fixture: " + classFile);

        Map<String, String> generatedSources = new HashMap<>();
        OutputSinkFactory outputSinks = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType type, Collection<SinkClass> available) {
                if (type == SinkType.JAVA && available.contains(SinkClass.DECOMPILED)) {
                    return Collections.singletonList(SinkClass.DECOMPILED);
                }
                return Collections.singletonList(SinkClass.STRING);
            }

            @Override
            public <T> Sink<T> getSink(final SinkType type, SinkClass sinkClass) {
                return value -> {
                    if (type == SinkType.JAVA && value instanceof SinkReturns.Decompiled) {
                        SinkReturns.Decompiled source = (SinkReturns.Decompiled) value;
                        generatedSources.put(source.getClassName(), source.getJava());
                    }
                };
            }
        };
        Map<String, String> options = new HashMap<>();
        options.put(OptionsImpl.SHOW_CFR_VERSION.getName(), "false");
        options.put(OptionsImpl.RENAME_SMALL_MEMBERS.getName(), "1");

        CfrDriver driver = new CfrDriver.Builder()
                .withOptions(options)
                .withOutputSink(outputSinks)
                .build();
        driver.analyse(Collections.singletonList(classFile.toAbsolutePath().toString()));

        String source = generatedSources.get("BoundedGenericFieldFixture");
        assertNotNull(source);
        assertTrue(source.contains("var_T_a"), source);
        assertTrue(source.contains("this.var_T_a"), source);

        Path generatedFile = temporaryDirectory.resolve("BoundedGenericFieldFixture.java");
        Files.write(generatedFile, source.getBytes(StandardCharsets.UTF_8));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);
        Path compiledDirectory = temporaryDirectory.resolve("compiled");
        Files.createDirectories(compiledDirectory);
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        int result = compiler.run(null, diagnostics, diagnostics, "-d",
                compiledDirectory.toString(), generatedFile.toString());

        assertEquals(0, result, new String(diagnostics.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void infersInheritedGenericFieldFromDescriptor(@TempDir Path temporaryDirectory) throws Exception {
        Path classes = Paths.get("target", "test-classes", "org", "benf", "cfr", "test");
        Path childClass = classes.resolve("InheritedBoundedGenericFixture.class");
        Path parentClass = classes.resolve("InheritedBoundedGenericParent.class");
        assertTrue(Files.isRegularFile(childClass), "Missing child fixture: " + childClass);
        assertTrue(Files.isRegularFile(parentClass), "Missing parent fixture: " + parentClass);

        Map<String, String> generatedSources = new HashMap<>();
        OutputSinkFactory sinks = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType type, Collection<SinkClass> available) {
                if (type == SinkType.JAVA && available.contains(SinkClass.DECOMPILED)) {
                    return Collections.singletonList(SinkClass.DECOMPILED);
                }
                return Collections.singletonList(SinkClass.STRING);
            }

            @Override
            public <T> Sink<T> getSink(final SinkType type, SinkClass sinkClass) {
                return value -> {
                    if (type == SinkType.JAVA && value instanceof SinkReturns.Decompiled) {
                        SinkReturns.Decompiled source = (SinkReturns.Decompiled) value;
                        generatedSources.put(source.getClassName(), source.getJava());
                    }
                };
            }
        };
        CfrDriver driver = new CfrDriver.Builder()
                .withOptions(Collections.singletonMap(OptionsImpl.SHOW_CFR_VERSION.getName(), "false"))
                .withOutputSink(sinks)
                .build();
        driver.analyse(Arrays.asList(childClass.toAbsolutePath().toString(),
                parentClass.toAbsolutePath().toString()));

        String child = generatedSources.get("InheritedBoundedGenericFixture");
        assertNotNull(child);
        Path generatedSource = temporaryDirectory.resolve("InheritedBoundedGenericFixture.java");
        Files.write(generatedSource, child.getBytes(StandardCharsets.UTF_8));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);
        Path compiledDirectory = temporaryDirectory.resolve("compiled");
        Files.createDirectories(compiledDirectory);
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        int result = compiler.run(null, diagnostics, diagnostics, "-classpath",
                Paths.get("target", "test-classes").toAbsolutePath().toString(),
                "-d", compiledDirectory.toString(), generatedSource.toString());

        assertEquals(0, result, new String(diagnostics.toByteArray(), StandardCharsets.UTF_8));
    }

    @Test
    void recompilesEnhancedForAfterErasingFieldGenerics(@TempDir Path temporaryDirectory) throws Exception {
        Path fixture = Paths.get("decompilation-test", "test-fixtures", "RawEnhancedForFieldFixture.java");
        assertTrue(Files.isRegularFile(fixture), "Missing fixture: " + fixture);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);
        Path inputDirectory = temporaryDirectory.resolve("input");
        Files.createDirectories(inputDirectory);
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        int initialResult = compiler.run(null, diagnostics, diagnostics, "-g:none",
                "-d", inputDirectory.toString(), fixture.toString());
        assertEquals(0, initialResult, new String(diagnostics.toByteArray(), StandardCharsets.UTF_8));

        Path classFile = inputDirectory.resolve(Paths.get("org", "benf", "cfr", "test",
                "RawEnhancedForFieldFixture.class"));
        assertTrue(Files.isRegularFile(classFile), "Missing compiled fixture: " + classFile);

        Map<String, String> sources = new HashMap<>();
        OutputSinkFactory sinks = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType type, Collection<SinkClass> available) {
                if (type == SinkType.JAVA && available.contains(SinkClass.DECOMPILED)) {
                    return Collections.singletonList(SinkClass.DECOMPILED);
                }
                return Collections.singletonList(SinkClass.STRING);
            }

            @Override
            public <T> Sink<T> getSink(final SinkType type, SinkClass sinkClass) {
                return value -> {
                    if (type == SinkType.JAVA && value instanceof SinkReturns.Decompiled) {
                        SinkReturns.Decompiled source = (SinkReturns.Decompiled) value;
                        sources.put(source.getClassName(), source.getJava());
                    }
                };
            }
        };
        CfrDriver driver = new CfrDriver.Builder()
                .withOptions(Collections.singletonMap(OptionsImpl.SHOW_CFR_VERSION.getName(), "false"))
                .withOutputSink(sinks)
                .build();
        driver.analyse(Collections.singletonList(classFile.toAbsolutePath().toString()));

        String source = sources.get("RawEnhancedForFieldFixture");
        assertNotNull(source);
        assertTrue(source.contains("for ("), source);
        assertTrue(source.contains(".forEach("), source);
        assertTrue(source.contains("String::trim"), source);
        assertTrue(source.contains("consumeWildcardCallback"), source);
        assertTrue(source.contains("consumeSuperWildcardCallback"), source);

        Path generatedSource = temporaryDirectory.resolve("RawEnhancedForFieldFixture.java");
        Files.write(generatedSource, source.getBytes(StandardCharsets.UTF_8));
        Path compiledDirectory = temporaryDirectory.resolve("compiled");
        Files.createDirectories(compiledDirectory);
        diagnostics.reset();
        int result = compiler.run(null, diagnostics, diagnostics, "-d",
                compiledDirectory.toString(), generatedSource.toString());
        assertEquals(0, result, new String(diagnostics.toByteArray(), StandardCharsets.UTF_8));
    }

}
