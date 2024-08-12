package org.jabref.logic.openoffice;

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
    void validParsing(String name, String expectedCitationKey, String expectedCitationNumber, String expectedUniqueId) {
        ReferenceMark referenceMark = new ReferenceMark(name);

        assertEquals(expectedCitationKey, referenceMark.getCitationKey());
        assertEquals(expectedCitationNumber, referenceMark.getCitationNumber());
        assertEquals(expectedUniqueId, referenceMark.getUniqueId());
    }

    private static Stream<Arguments> validParsing() {
        return Stream.of(
                Arguments.of("JABREF_key1 CID_12345 uniqueId1", "key1", "12345", "uniqueId1"),
                Arguments.of("JABREF_key2 CID_67890 uniqueId2", "key2", "67890", "uniqueId2"),
                Arguments.of("JABREF_key3 CID_54321 uniqueId3", "key3", "54321", "uniqueId3")
        );
    }
}
