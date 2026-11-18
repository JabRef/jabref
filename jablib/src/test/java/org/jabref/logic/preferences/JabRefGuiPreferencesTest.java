package org.jabref.logic.preferences;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JabRefGuiPreferencesTest {
    private static Stream<Arguments> provideTestData() {
        return Stream.of(
                Arguments.of(List.of("A", "B", "C", "D"), "A;B;C;D"),
                Arguments.of(List.of("A", "B", "C", ""), "A;B;C;"),
                Arguments.of(List.of("a;b", "C:\\Tools\\app.exe"), "a\\;b;C:\\\\Tools\\\\app.exe"),
                Arguments.of(List.of("a\\", "b"), "a\\\\;b")
        );
    }

    @ParameterizedTest
    @MethodSource("provideTestData")
    void convertStringToList(List<String> sampleList, String sampleString) {
        assertEquals(sampleList, JabRefCliPreferences.convertStringToList(sampleString));
    }

    @ParameterizedTest
    @MethodSource("provideTestData")
    void convertListToString(List<String> sampleList, String sampleString) {
        assertEquals(sampleString, JabRefCliPreferences.convertListToString(sampleList));
    }

    @Test
    void convertStringToListPreservesUnescapedBackslashes() {
        assertEquals(List.of("C:\\Tools\\app.exe"), JabRefCliPreferences.convertStringToList("C:\\Tools\\app.exe"));
    }

    @Test
    void suppliedPreferencesNodesKeepValuesAndChildNodesSeparate() throws BackingStoreException {
        Preferences root = Preferences.userRoot().node("/org/jabref-preferences-test-" + UUID.randomUUID());
        try {
            JabRefCliPreferences first = new JabRefCliPreferences(root.node("first"));
            JabRefCliPreferences second = new JabRefCliPreferences(root.node("second"));
            first.put("isolatedValue", "first");
            second.put("isolatedValue", "second");
            first.addDeclinedCustomEntryTypes(Set.of("article"));

            assertEquals("first", new JabRefCliPreferences(root.node("first")).get("isolatedValue", ""));
            assertEquals("second", second.get("isolatedValue", ""));
            assertEquals(Set.of("article"), first.getDeclinedCustomEntryTypes());
            assertEquals(Set.of(), second.getDeclinedCustomEntryTypes());
        } finally {
            root.removeNode();
        }
    }
}
