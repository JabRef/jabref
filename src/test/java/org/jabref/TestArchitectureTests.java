package org.jabref;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.jabref.MainArchitectureTests.CLASS_ORG_JABREF_GLOBALS;

@RunWith(Parameterized.class)
public class TestArchitectureTests {

    private static final String CLASS_ORG_JABREF_PREFERENCES = "org.jabref.preferences.JabRefPreferences";
    private static final String CLASS_ORG_JABREF_PREFERENCES_TEST = "JabRefPreferencesTest";

    private final String forbiddenPackage;

    private List<String> exceptions;

    public TestArchitectureTests(String forbiddenPackage) {
        this.forbiddenPackage = forbiddenPackage;

        // Add exceptions for the architectural test here
        // Note that bending the architectural constraints should not be done inconsiderately
        exceptions = new ArrayList<>();
        exceptions.add(CLASS_ORG_JABREF_PREFERENCES_TEST);
    }

    @Parameterized.Parameters(name = "tests independent of {0}?")
    public static Iterable<Object[]> data() {
        return Arrays.asList(
                new Object[][]{
                        {CLASS_ORG_JABREF_PREFERENCES},
                        {CLASS_ORG_JABREF_GLOBALS}
                }
        );
    }

    @Test
    public void testsAreIndependent() throws IOException {
        Predicate<String> isForbiddenPackage = (s) -> s.startsWith("import " + forbiddenPackage);
        Predicate<String> isExceptionClass = (s) -> exceptions.stream().anyMatch(exception -> s.startsWith("public class " + exception));

        List<Path> files = Files.walk(Paths.get("src/test/"))
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> {
                    try {
                        return Files.readAllLines(p, StandardCharsets.UTF_8).stream().noneMatch(isExceptionClass);
                    } catch (IOException e) {
                        return false;
                    }
                })
                .filter(p -> {
                    try {
                        return Files.readAllLines(p, StandardCharsets.UTF_8).stream().anyMatch(isForbiddenPackage);
                    } catch (IOException e) {
                        return false;
                    }
                }).collect(Collectors.toList());

        Assert.assertEquals("The following classes are not allowed to depend on " + forbiddenPackage,
                Collections.emptyList(), files);
    }
}
