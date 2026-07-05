package org.jabref.logic.preferences;

import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.util.io.DirectoryMapping;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JabRefGuiPreferencesTest {
    private static Stream<Arguments> provideTestData() {
        return Stream.of(
                Arguments.of(List.of("A", "B", "C", "D"), "A;B;C;D"),
                Arguments.of(List.of("A", "B", "C", ""), "A;B;C;")
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
    void convertDirectoryMappingsRoundTrip() {
        List<DirectoryMapping> mappings = List.of(
                new DirectoryMapping("/old/literature", "/new/literature"),
                new DirectoryMapping("/old/literature", "D:\\literature"));

        String serialized = JabRefCliPreferences.convertDirectoryMappingsToString(mappings);

        assertEquals(mappings, JabRefCliPreferences.convertStringToDirectoryMappings(serialized));
    }

    @Test
    void convertStringToDirectoryMappingsEmptyStringYieldsEmptyList() {
        assertEquals(List.of(), JabRefCliPreferences.convertStringToDirectoryMappings(""));
    }
}
