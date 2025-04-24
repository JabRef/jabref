package org.jabref.logic.preferences;

import java.util.List;
import java.util.stream.Stream;

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
}
