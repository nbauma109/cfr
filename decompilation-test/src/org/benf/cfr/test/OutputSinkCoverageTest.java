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
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputSinkCoverageTest {
    private static final String FIXTURE = "CoverageSinkFixture";

    @Test
    void tokenStreamRecompilesAndPreservesIdentifierReferences(@TempDir Path temporaryDirectory) throws Exception {
        List<TokenSnapshot> tokens = new ArrayList<>();
        OutputSinkFactory factory = new OutputSinkFactory() {
            @Override
            public List<SinkClass> getSupportedSinks(SinkType type, Collection<SinkClass> available) {
                if (type == SinkType.JAVA) {
                    return Collections.singletonList(SinkClass.TOKEN_STREAM);
                }
                return Collections.singletonList(SinkClass.STRING);
            }

            @Override
            public <T> Sink<T> getSink(SinkType type, SinkClass sinkClass) {
                return value -> {
                    if (type == SinkType.JAVA) {
                        tokens.add(new TokenSnapshot((SinkReturns.Token) value));
                    }
                };
            }
        };
        driver(factory, false).analyse(Collections.singletonList(fixtureClassFile()));

        assertTrue(tokens.size() > 15, "Token stream is unexpectedly empty");
        assertEquals(SinkReturns.TokenType.EOF, tokens.get(tokens.size() - 1).type);
        assertEquals(1, countTokens(tokens, SinkReturns.TokenType.EOF));
        assertTrue(containsToken(tokens, SinkReturns.TokenType.KEYWORD, "class "));
        assertTrue(containsDefinedToken(tokens, SinkReturns.TokenType.FIELD, "count"));
        assertTrue(containsDefinedToken(tokens, SinkReturns.TokenType.METHOD, "increment"));
        assertTrue(containsToken(tokens, SinkReturns.TokenType.LITERAL, "7"));
        assertTrue(containsToken(tokens, SinkReturns.TokenType.INDENT, ""));
        assertTrue(containsToken(tokens, SinkReturns.TokenType.UNINDENT, ""));
        assertTrue(containsToken(tokens, SinkReturns.TokenType.COMMENT, "/* "));
        assertTrue(hasCorrelatedIdentifier(tokens), "Identifier references lost their declaration identity");

        StringBuilder source = new StringBuilder();
        for (TokenSnapshot token : tokens) {
            if (!token.type.isControl()) {
                source.append(token.text);
            }
        }

        Path sourceFile = temporaryDirectory.resolve(Paths.get("org", "benf", "cfr",
                "test", FIXTURE + ".java"));
        Files.createDirectories(sourceFile.getParent());
        Files.write(sourceFile, source.toString().getBytes(StandardCharsets.UTF_8));

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "The regression test requires a Java compiler");
        Path compiledDirectory = temporaryDirectory.resolve("compiled");
        Files.createDirectories(compiledDirectory);
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        int result = compiler.run(null, diagnostics, diagnostics, "-d",
                compiledDirectory.toString(), sourceFile.toString());
        assertEquals(0, result, new String(diagnostics.toByteArray(), StandardCharsets.UTF_8));

        try (URLClassLoader loader = new URLClassLoader(
                new URL[]{compiledDirectory.toUri().toURL()}, null)) {
            Class<?> compiledType = Class.forName("org.benf.cfr.test." + FIXTURE, true, loader);
            Object instance = compiledType.getDeclaredConstructor().newInstance();
            assertEquals(20, compiledType.getMethod("twice", int.class).invoke(instance, 2));
        }
    }

    @Test
    void nullSinksDiscardOutputWithoutThrowing() {
        for (OutputSinkFactory.SinkClass javaSink : new OutputSinkFactory.SinkClass[]{
                OutputSinkFactory.SinkClass.STRING,
                OutputSinkFactory.SinkClass.DECOMPILED,
                OutputSinkFactory.SinkClass.DECOMPILED_MULTIVER,
                OutputSinkFactory.SinkClass.TOKEN_STREAM}) {
            OutputSinkFactory factory = new OutputSinkFactory() {
                @Override
                public List<SinkClass> getSupportedSinks(SinkType type, Collection<SinkClass> available) {
                    if (type == SinkType.JAVA) {
                        return Collections.singletonList(javaSink);
                    }
                    if (type == SinkType.LINENUMBER) {
                        return Collections.singletonList(SinkClass.LINE_NUMBER_MAPPING);
                    }
                    return Collections.singletonList(SinkClass.STRING);
                }

                @Override
                public <T> Sink<T> getSink(SinkType type, SinkClass sinkClass) {
                    return null;
                }
            };

            assertDoesNotThrow(() -> driver(factory, true)
                    .analyse(Collections.singletonList(fixtureClassFile())), javaSink.name());
        }
    }

    private static CfrDriver driver(OutputSinkFactory factory, boolean includeLineNumbers) {
        Map<String, String> options = new HashMap<>();
        options.put(OptionsImpl.SHOW_CFR_VERSION.getName(), "false");
        options.put(OptionsImpl.TRACK_BYTECODE_LOC.getName(), Boolean.toString(includeLineNumbers));
        return new CfrDriver.Builder().withOptions(options).withOutputSink(factory).build();
    }

    private static String fixtureClassFile() {
        return Paths.get("target", "test-classes", "org", "benf", "cfr", "test",
                FIXTURE + ".class").toAbsolutePath().toString();
    }

    private static int countTokens(List<TokenSnapshot> tokens, SinkReturns.TokenType type) {
        int result = 0;
        for (TokenSnapshot token : tokens) {
            if (token.type == type) {
                result++;
            }
        }
        return result;
    }

    private static boolean containsToken(List<TokenSnapshot> tokens, SinkReturns.TokenType type, String text) {
        for (TokenSnapshot token : tokens) {
            if (token.type == type && text.equals(token.text)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsDefinedToken(List<TokenSnapshot> tokens, SinkReturns.TokenType type,
                                                String text) {
        for (TokenSnapshot token : tokens) {
            if (token.type == type && text.equals(token.text) &&
                    token.flags.contains(SinkReturns.TokenTypeFlags.DEFINES)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasCorrelatedIdentifier(List<TokenSnapshot> tokens) {
        Set<Object> declarations = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        for (TokenSnapshot token : tokens) {
            if (token.type == SinkReturns.TokenType.IDENTIFIER &&
                    token.flags.contains(SinkReturns.TokenTypeFlags.DEFINES) && token.rawValue != null) {
                declarations.add(token.rawValue);
            }
        }
        for (TokenSnapshot token : tokens) {
            if (token.type == SinkReturns.TokenType.IDENTIFIER &&
                    !token.flags.contains(SinkReturns.TokenTypeFlags.DEFINES) &&
                    declarations.contains(token.rawValue)) {
                return true;
            }
        }
        return false;
    }

    private static final class TokenSnapshot {
        private final SinkReturns.TokenType type;
        private final String text;
        private final Object rawValue;
        private final Set<SinkReturns.TokenTypeFlags> flags;

        private TokenSnapshot(SinkReturns.Token token) {
            this.type = token.getTokenType();
            this.text = token.getText();
            this.rawValue = token.getRawValue();
            this.flags = token.getFlags().isEmpty()
                    ? Collections.<SinkReturns.TokenTypeFlags>emptySet()
                    : EnumSet.copyOf(token.getFlags());
        }
    }
}
