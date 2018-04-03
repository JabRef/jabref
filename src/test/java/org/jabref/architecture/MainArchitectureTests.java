package org.jabref.architecture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MainArchitectureTests {

    public static final String CLASS_ORG_JABREF_GLOBALS = "org.jabref.Globals";
    private static final String PACKAGE_JAVAX_SWING = "javax.swing";
    private static final String PACKAGE_JAVA_AWT = "java.awt";
    private static final String PACKAGE_JAVA_FX = "javafx";
    private static final String PACKAGE_ORG_JABREF_GUI = "org.jabref.gui";
    private static final String PACKAGE_ORG_JABREF_LOGIC = "org.jabref.logic";
    private static final String PACKAGE_ORG_JABREF_MODEL = "org.jabref.model";
    private static final String EXCEPTION_PACKAGE_JAVA_AWT_GEOM = "java.awt.geom";
    private static final String EXCEPTION_PACKAGE_JAVA_FX_COLLECTIONS = "javafx.collections";
    private static final String EXCEPTION_PACKAGE_JAVA_FX_BEANS = "javafx.beans";
    private static final String EXCEPTION_CLASS_JAVA_FX_COLOR = "javafx.scene.paint.Color";
    private static final String EXCEPTION_CLASS_JAVA_FX_PAIR = "javafx.util.Pair";

    private static Map<String, List<String>> exceptions;

    @BeforeAll
    public static void setUp() {
        exceptions = new HashMap<>();
        // Add exceptions for the architectural test here
        // Note that bending the architectural constraints should not be done inconsiderately

        List<String> logicExceptions = new ArrayList<>(4);
        logicExceptions.add(EXCEPTION_PACKAGE_JAVA_AWT_GEOM);
        logicExceptions.add(EXCEPTION_PACKAGE_JAVA_FX_COLLECTIONS);
        logicExceptions.add(EXCEPTION_PACKAGE_JAVA_FX_BEANS);
        logicExceptions.add(EXCEPTION_CLASS_JAVA_FX_COLOR);
        logicExceptions.add(EXCEPTION_CLASS_JAVA_FX_PAIR);

        List<String> modelExceptions = new ArrayList<>(4);
        modelExceptions.add(EXCEPTION_PACKAGE_JAVA_FX_COLLECTIONS);
        modelExceptions.add(EXCEPTION_CLASS_JAVA_FX_COLOR);
        modelExceptions.add(EXCEPTION_PACKAGE_JAVA_FX_COLLECTIONS);
        modelExceptions.add(EXCEPTION_PACKAGE_JAVA_FX_BEANS);

        exceptions.put(PACKAGE_ORG_JABREF_LOGIC, logicExceptions);
        exceptions.put(PACKAGE_ORG_JABREF_MODEL, modelExceptions);
    }

    public static Stream<Arguments> getPackages() {

        return Stream.of(
                Arguments.of(PACKAGE_ORG_JABREF_LOGIC, PACKAGE_JAVA_AWT),
                Arguments.of(PACKAGE_ORG_JABREF_LOGIC, PACKAGE_JAVAX_SWING),
                Arguments.of(PACKAGE_ORG_JABREF_LOGIC, PACKAGE_JAVA_FX),
                Arguments.of(PACKAGE_ORG_JABREF_LOGIC, PACKAGE_ORG_JABREF_GUI),
                Arguments.of(PACKAGE_ORG_JABREF_LOGIC, CLASS_ORG_JABREF_GLOBALS),

                Arguments.of(PACKAGE_ORG_JABREF_MODEL, PACKAGE_JAVA_AWT),
                Arguments.of(PACKAGE_ORG_JABREF_MODEL, PACKAGE_JAVAX_SWING),
                Arguments.of(PACKAGE_ORG_JABREF_MODEL, PACKAGE_JAVA_FX),
                Arguments.of(PACKAGE_ORG_JABREF_MODEL, PACKAGE_ORG_JABREF_GUI),
                Arguments.of(PACKAGE_ORG_JABREF_MODEL, PACKAGE_ORG_JABREF_LOGIC),
                Arguments.of(PACKAGE_ORG_JABREF_MODEL, CLASS_ORG_JABREF_GLOBALS));
    }

    @ParameterizedTest(name = "{index} -- is {0} independent of {1}?")
    @MethodSource("getPackages")
    public void firstPackageIsIndependentOfSecondPackage(String firstPackage, String secondPackage) throws IOException {
        Predicate<String> isExceptionPackage = (s) -> (s.startsWith("import " + secondPackage)
                || s.startsWith("import static " + secondPackage))
                && exceptions.getOrDefault(firstPackage, Collections.emptyList())
                        .stream()
                        .noneMatch(exception -> s.startsWith("import " + exception));

        Predicate<String> isPackage = (s) -> s.startsWith("package " + firstPackage);

        try (Stream<Path> pathStream = Files.walk(Paths.get("src/main/"))) {
            List<Path> files = pathStream
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
                    })
                    .collect(Collectors.toList());

            assertEquals(Collections.emptyList(), files, "The following classes are not allowed to depend on " + secondPackage);
        }
    }
}
