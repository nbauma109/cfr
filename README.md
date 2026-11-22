# CFR - Another Java Decompiler \o/

This is the public repository for the CFR decompiler, main site hosted at [benf.org/other/cfr](https://www.benf.org/other/cfr)

CFR will decompile modern Java features - [including much of Java 9](https://www.benf.org/other/cfr/java9observations.html), [12](https://www.benf.org/other/cfr/switch_expressions.html) & [14](https://www.benf.org/other/cfr/java14instanceof_pattern), but is written entirely in Java 6, so will work anywhere!  ([FAQ](https://www.benf.org/other/cfr/faq.html)) - It'll even make a decent go of turning class files from other JVM languages back into java!

To use, simply run the specific version jar, with the class name(s) you want to decompile (either as a path to a class file, or as a fully qualified classname on your classpath).
(`--help` to list arguments).

Alternately, to decompile an entire jar, simply provide the jar path, and if you want to emit files (which you probably do!) add `--outputdir /tmp/putithere`.

# Getting CFR

The main site for CFR is [benf.org/other/cfr](https://www.benf.org/other/cfr), where releases are available with a bunch of rambling musings from the author.

Since 0.145, Binaries are [published on GitHub along with release tags](https://github.com/leibnitz27/cfr/releases).

You can also download CFR from [Maven Central](https://central.sonatype.com/artifact/org.benf/cfr), though releases are published a few days late usually, to allow for release regret.

SNAPSHOT versions can be obtained from the [GitHub workflow artifacts](https://github.com/leibnitz27/cfr/actions/workflows/maven.yml?query=branch%3Amaster), see also the [GitHub docs](https://docs.github.com/en/actions/managing-workflow-runs/downloading-workflow-artifacts).

# Issues

If you have an issue, please **_DO NOT_** include copyright materials.  I will have to delete your issue.

# Building CFR

Dead easy!

Just ensure you have [Maven](https://maven.apache.org/) installed. Then `mvn compile` in the root directory of this project will get you what you need.

Note: If you encounter a `maven-compiler-plugin...: Compilation failure` error while trying to compile the project then your `JAVA_HOME` environment variable is probably pointing to a JDK version that doesn't support `6` for the `source` or `target` compile options.
Fix this by pointing `JAVA_HOME` to a JDK version that still supports compiling to Java 1.6 such as JDK 11. Also note, the version of Java on your `JAVA` may need to be greater than 1.6 if you are using Maven version `>=3.3.1` which requires Java 1.7. The best solution is to use JDK 8, 9, 10 or 11 for both your `PATH` and `JAVA_HOME`.

The main class is `org.benf.cfr.reader.Main`, so once you've built, you can test it out (from `target/classes`)
```
java org.benf.cfr.reader.Main java.lang.Object
```
to get CFR to decompile `java.lang.Object`.


## Decompilation tests

As part of the Maven build automatic decompilation tests are performed. They verify that the current decompiled output of CFR matches the expected previous output. The compiled test inputs are part of a separate Git repository; it is therefore necessary to clone this repository with `git clone --recurse-submodules`. The expected output and XML test specifications are part of this repository so they can be updated independently of the test data. The test data is in `decompilation-test/test-data`, the expected results are in `decompilation-test/test-data-expected-output`, and the selected test cases are defined in `decompilation-test/test-specs/classes.xml` (see [Test specifications](#test-specifications) below).

The decompilation tests are also performed by the GitHub workflow, and in case of test failures the unified diff is available in a [workflow artifact](https://docs.github.com/en/actions/managing-workflow-runs/downloading-workflow-artifacts) called "decompilation-test-failures-diff".

**The expected output is not the gold standard**, it merely describes the currently expected output. There is nothing wrong with adjusting the expected output, if the changes to the decompilation results are reasonable.

The test class is [`org.benf.cfr.test.DecompilationTest`](decompilation-test/src/org/benf/cfr/test/DecompilationTest.java). To add or change class-file test cases, edit the XML specifications rather than the test class. Additionally it is possible to directly execute the tests there from the IDE. This usually gives better output than what is shown by Maven, and allows using the built-in IDE functionality for showing differences between the expected and the actual data.

### Test specifications

The tests to run are selected in
[`decompilation-test/test-specs/classes.xml`](decompilation-test/test-specs/classes.xml).
Each `<class>` entry identifies a compiled class in the test-data submodule:

```xml
<class>
    <path>precompiled_tests/java_6/org/benf/cfr/tests</path>
    <name>CondJumpTest2c</name>
    <label>6</label>
    <options>
        <option key="usenametable" value="false"/>
    </options>
</class>
```

The `path` is relative to `decompilation-test/test-data`, and `name` identifies the
`.class` file without its extension. The `label` distinguishes test cases (for example,
the compiler version). `options` is optional; each `option` supplies a CFR command-line
option using `key` and `value` attributes. Separate `.options` files are not supported.

### Expected output

For each specified class, expected output lives in
`decompilation-test/test-data-expected-output/classes/`. The file prefix is
`<name>.<label>`, so the example above uses:

- `CondJumpTest2c.6.expected.java` — expected decompiled Java source
- `CondJumpTest2c.6.expected.summary` — expected CFR API summary, if any
- `CondJumpTest2c.6.expected.exceptions` — expected CFR API exceptions, if any

The Java source file is required. Summary and exception files are only necessary
when CFR reports the corresponding output. The current XML-driven Maven test
provider selects class-file cases; it does not select JAR fixtures.

### Decompilation note comments

The expected Java output files support comments representing _decompilation notes_. They are ignored during comparison with the actual Java output and can for example be used to indicate incorrect or improvable CFR output. There are two kinds of decompilation notes:

- Line: Start with `//#` (optionally prefixed with whitespace)
- Inline: `/*# ... #*/`

Line decompilation notes should be used sparingly, especially in large files, because they shift line numbers for the diff files (due to being removed during comparison), which can be confusing.

Example:
```java
//# Line decompilation note
public class MyClass {
    public static void main(String[] stringArray/*# Inline decompilation note #*/) {
        ...
    }
}
```

### Updating / creating expected data

When adding a lot of new classes or JAR files for the decompilation tests or when a change to CFR affects the output for a lot of classes or JAR files, manually creating or updating the expected output can be rather cumbersome. For these cases the following system properties exist which help with this. They can be set with `-D<system-property>` when running tests. However, when these system properties are set, the respective tests will still fail (but the expected data is updated) to prevent accidentally using them for regular test execution.

- `cfr.decompilation-test.create-expected`  
Generates all missing expected test data based on the current CFR output.
- `cfr.decompilation-test.update-expected`  
Updates the expected test data to match the actual data produced by CFR. Note that this does not work for expected Java output using [decompilation notes](#decompilation-note-comments) because those comments would get lost. The affected tests have to be updated manually.
