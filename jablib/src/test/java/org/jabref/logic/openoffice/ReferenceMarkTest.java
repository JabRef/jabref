package org.jabref.logic.openoffice;

import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.openoffice.oocsltext.CSLCitationType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReferenceMarkTest {

    @ParameterizedTest
    @MethodSource
    @DisplayName("Test parsing of valid reference marks")
    void validParsing(String name, List<String> expectedCitationKeys, List<Integer> expectedCitationNumbers, String expectedUniqueId, CSLCitationType expectedCitationType) {
        ReferenceMark referenceMark = new ReferenceMark(name);

        assertEquals(expectedCitationKeys, referenceMark.getCitationKeys());
        assertEquals(expectedCitationNumbers, referenceMark.getCitationNumbers());
        assertEquals(expectedUniqueId, referenceMark.getUniqueId());
        assertEquals(expectedCitationType, referenceMark.getCitationType());
    }

    private static Stream<Arguments> validParsing() {
        return Stream.of(
                // Single citation cases
                Arguments.of(
                        "JABREF_key1 CID_12345 uniqueId1",
                        List.of("key1"), List.of(12345), "uniqueId1", CSLCitationType.NORMAL
                ),
                Arguments.of(
                        "JABREF_key2 CID_67890 uniqueId2 NORMAL",
                        List.of("key2"), List.of(67890), "uniqueId2", CSLCitationType.NORMAL
                ),

                // Multiple citation cases
                Arguments.of(
                        "JABREF_key3 CID_54321, JABREF_key4 CID_98765 uniqueId3 NORMAL",
                        List.of("key3", "key4"), List.of(54321, 98765), "uniqueId3", CSLCitationType.NORMAL
                ),
                Arguments.of(
                        "JABREF_key5 CID_11111, JABREF_key6 CID_22222, JABREF_key7 CID_33333 uniqueId4 NORMAL",
                        List.of("key5", "key6", "key7"), List.of(11111, 22222, 33333), "uniqueId4", CSLCitationType.NORMAL
                ),

                // Non-standard citation key
                Arguments.of(
                        "JABREF_willberg-forssman:1997b CID_5 yov3b0su NORMAL",
                        List.of("willberg-forssman:1997b"), List.of(5), "yov3b0su", CSLCitationType.NORMAL
                ),
                Arguments.of(
                        "JABREF_PGF/TikZTeam2023 CID_8 kyu75a4s NORMAL",
                        List.of("PGF/TikZTeam2023"), List.of(8), "kyu75a4s", CSLCitationType.NORMAL
                ),

                // Unicode citation keys (Cyrillic)
                Arguments.of(
                        "JABREF_Ты2025 CID_1 abc123 NORMAL",
                        List.of("Ты2025"), List.of(1), "abc123", CSLCitationType.NORMAL
                ),
                Arguments.of(
                        "JABREF_Я2025 CID_2 def456 NORMAL",
                        List.of("Я2025"), List.of(2), "def456", CSLCitationType.NORMAL
                ),

                // Endashes and emdashes
                Arguments.of(
                        "JABREF_DeutscherParitaetischerWohlfahrtsverband–Gesamtverband2016 CID_3 kku224 NORMAL",
                        List.of("DeutscherParitaetischerWohlfahrtsverband–Gesamtverband2016"), List.of(3), "kku224", CSLCitationType.NORMAL
                ),
                Arguments.of(
                        "JABREF_Em—dash CID_2 iijhy4 NORMAL",
                        List.of("Em—dash"), List.of(2), "iijhy4", CSLCitationType.NORMAL
                ),
                Arguments.of(
                        "JABREF_Keen_2011 CID_100 cite NORMAL", // for LO extension
                        List.of("Keen_2011"), List.of(100), "cite", CSLCitationType.NORMAL
                ),
                Arguments.of(
                        "JABREF_legacyKey CID_42 legacyUniqueId",
                        List.of("legacyKey"), List.of(42), "legacyUniqueId", CSLCitationType.NORMAL
                ),
                Arguments.of(
                        "JABREF_key8 CID_7 uniqueId8 IN_TEXT",
                        List.of("key8"), List.of(7), "uniqueId8", CSLCitationType.IN_TEXT
                ),
                Arguments.of(
                        "JABREF_key9 CID_8 uniqueId9 EMPTY",
                        List.of("key9"), List.of(8), "uniqueId9", CSLCitationType.EMPTY
                )
        );
    }
}
