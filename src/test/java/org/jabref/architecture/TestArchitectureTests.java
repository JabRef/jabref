package org.jabref.architecture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.jabref.architecture.MainArchitectureTests.CLASS_ORG_JABREF_GLOBALS;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestArchitectureTests {

    private static final String CLASS_ORG_JABREF_PREFERENCES = "org.jabref.preferences.JabRefPreferences";
    private static final String CLASS_ORG_JABREF_PREFERENCES_TEST = "JabRefPreferencesTest";
    private static final String CLASS_ORG_JABREF_PREFERENCES_MIGRATIONS_TEST = "PreferencesMigrationsTest";
    private static final String CLASS_ORG_JABREF_SAVE_DATABASE_ACTION_TEST = "SaveDatabaseActionTest";
    private static final String CLASS_ORG_JABREF_UPDATE_TIMESTAMP_LISTENER_TEST = "UpdateTimestampListenerTest";
    private static final String CLASS_ORG_JABREF_ENTRY_EDITOR_TEST = "EntryEditorTest";
    private static final String CLASS_ORG_JABREF_LINKED_FILE_VIEW_MODEL_TEST = "LinkedFileViewModelTest";
    private static final String CLASS_ORG_JABREF_SCREENSHOTS_TEST = "Screenshots";
    private static final String CLASS_ORG_JABREF_SCREENSHOT_DARK_THEME_TEST = "ScreenshotDarkTheme";

    private final List<String> exceptions;

    public TestArchitectureTests() {
        // Add exceptions for the architectural test here
        // Note that bending the architectural constraints should not be done inconsiderately
        exceptions = List.of(
                CLASS_ORG_JABREF_PREFERENCES_TEST,
                CLASS_ORG_JABREF_PREFERENCES_MIGRATIONS_TEST,
                CLASS_ORG_JABREF_SAVE_DATABASE_ACTION_TEST,
                CLASS_ORG_JABREF_UPDATE_TIMESTAMP_LISTENER_TEST,
                CLASS_ORG_JABREF_ENTRY_EDITOR_TEST,
                CLASS_ORG_JABREF_LINKED_FILE_VIEW_MODEL_TEST,
                CLASS_ORG_JABREF_SCREENSHOTS_TEST,
                CLASS_ORG_JABREF_SCREENSHOT_DARK_THEME_TEST);
    }

    public static Stream<String> forbiddenPackages() {
        return Stream.of(CLASS_ORG_JABREF_PREFERENCES, CLASS_ORG_JABREF_GLOBALS);
    }

    @ParameterizedTest
    @MethodSource("forbiddenPackages")
    public void testsAreIndependent(String forbiddenPackage) throws IOException {
        Predicate<String> isForbiddenPackage = (s) -> s.startsWith("import " + forbiddenPackage);
        Predicate<String> isExceptionClass = (s) -> exceptions.stream().anyMatch(exception -> s.startsWith("class " + exception));

        try (Stream<Path> pathStream = Files.walk(Paths.get("src/test/"))) {
            List<Path> files = pathStream
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

            assertEquals(Collections.emptyList(), files, "The following classes are not allowed to depend on " + forbiddenPackage);
        }
    }
}
