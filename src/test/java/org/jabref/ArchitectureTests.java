package org.jabref;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ArchitectureTests {

    private static final String PACKAGE_JAVAX_SWING = "javax.swing";
    private static final String PACKAGE_JAVA_AWT = "java.awt";
    private static final String PACKAGE_ORG_JABREF_GUI = "org.jabref.gui";
    private static final String PACKAGE_ORG_JABREF_LOGIC = "org.jabref.logic";
    private static final String PACKAGE_ORG_JABREF_MODEL = "org.jabref.model";
    private static final String CLASS_ORG_JABREF_GLOBALS = "org.jabref.Globals";

    private static final String EXCEPTION_PACKAGE_JAVA_AWT_GEOM = "java.awt.geom";
    private final String firstPackage;
    private final String secondPackage;
    private Map<String, List<String>> exceptions;

    public ArchitectureTests(String firstPackage, String secondPackage) {
        this.firstPackage = firstPackage;
        this.secondPackage = secondPackage;

        // Add exceptions for the architectural test here
        // Note that bending the architectural constraints should not be done inconsiderately
        exceptions = new HashMap<>();
        exceptions.put(PACKAGE_ORG_JABREF_LOGIC,
                Collections.singletonList(EXCEPTION_PACKAGE_JAVA_AWT_GEOM));
    }


    @Parameterized.Parameters(name = "{index} -- is {0} independent of {1}?")
    public static Iterable<Object[]> data() {
        return Arrays.asList(
                new Object[][] {
                        {PACKAGE_ORG_JABREF_LOGIC, PACKAGE_JAVA_AWT},
                        {PACKAGE_ORG_JABREF_LOGIC, PACKAGE_JAVAX_SWING},
                        {PACKAGE_ORG_JABREF_LOGIC, PACKAGE_ORG_JABREF_GUI},
                        {PACKAGE_ORG_JABREF_LOGIC, CLASS_ORG_JABREF_GLOBALS},

                        {PACKAGE_ORG_JABREF_MODEL, PACKAGE_JAVA_AWT},
                        {PACKAGE_ORG_JABREF_MODEL, PACKAGE_JAVAX_SWING},
                        {PACKAGE_ORG_JABREF_MODEL, PACKAGE_ORG_JABREF_GUI},
                        {PACKAGE_ORG_JABREF_MODEL, PACKAGE_ORG_JABREF_LOGIC},
                        {PACKAGE_ORG_JABREF_MODEL, CLASS_ORG_JABREF_GLOBALS}
                }
        );
    }

    @Test
    public void firstPackageIsIndependentOfSecondPackage() throws IOException {
        Predicate<String> isExceptionPackage = (s) ->
                s.startsWith("import " + secondPackage)
                        && exceptions.getOrDefault(firstPackage, Collections.emptyList()).stream()
                        .noneMatch(exception -> s.startsWith("import " + exception));

        Predicate<String> isPackage = (s) -> s.startsWith("package " + firstPackage);

        List<Path> files = Files.walk(Paths.get("src"))
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> {
                    try {
                        return Files.readAllLines(p, StandardCharsets.UTF_8).stream().anyMatch(isPackage);
                    } catch (IOException e) {
                        return false;
                    }
                })
                .filter(p -> {
                    try {
                        return Files.readAllLines(p, StandardCharsets.UTF_8).stream().anyMatch(isExceptionPackage);
                    } catch (IOException e) {
                        return false;
                    }
                }).collect(Collectors.toList());

        Assert.assertEquals("The following classes are not allowed to depend on " + secondPackage,
                Collections.emptyList(), files);
    }

}
