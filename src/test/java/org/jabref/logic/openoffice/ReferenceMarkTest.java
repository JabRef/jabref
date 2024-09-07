package org.jabref.logic.openoffice;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReferenceMarkTest {

    @ParameterizedTest
    @MethodSource
    @DisplayName("Test parsing of valid reference marks")
    void validParsing(String name, List<String> expectedCitationKeys, List<Integer> expectedCitationNumbers, String expectedUniqueId) {
        ReferenceMark referenceMark = new ReferenceMark(name);

        assertEquals(expectedCitationKeys, referenceMark.getCitationKeys());
        assertEquals(expectedCitationNumbers, referenceMark.getCitationNumbers());
        assertEquals(expectedUniqueId, referenceMark.getUniqueId());
    }

    private static Stream<Arguments> validParsing() {
        return Stream.of(
                // Single citation cases
                Arguments.of(
                        "JABREF_key1 CID_12345 uniqueId1",
                        List.of("key1"), List.of(12345), "uniqueId1"
                ),
                Arguments.of(
                        "JABREF_key2 CID_67890 uniqueId2",
                        List.of("key2"), List.of(67890), "uniqueId2"
                ),

                // Multiple citation cases
                Arguments.of(
                        "JABREF_key3 CID_54321, JABREF_key4 CID_98765 uniqueId3",
                        List.of("key3", "key4"), List.of(54321, 98765), "uniqueId3"
                ),
                Arguments.of(
                        "JABREF_key5 CID_11111, JABREF_key6 CID_22222, JABREF_key7 CID_33333 uniqueId4",
                        List.of("key5", "key6", "key7"), List.of(11111, 22222, 33333), "uniqueId4"
                )
        );
    }
}
