package org.jabref.logic.openoffice.engine;

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Stream;

import org.jabref.logic.openoffice.ZoteroReferenceMark;
import org.jabref.model.openoffice.style.CitationType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@NullMarked
class MarkFormatIsolationTest {

    @ParameterizedTest
    @MethodSource("markCases")
    void canonicalMarksPreserveCitationKeyCharacters(String legacyName, List<String> keys, String uid, CitationType type) throws MarkCodecException {
        StringJoiner names = new StringJoiner(", ");
        keys.forEach(key -> names.add("JABREF_" + key));
        String wireType = switch (type) {
            case AUTHORYEAR_PAR ->
                    "NORMAL";
            case AUTHORYEAR_INTEXT ->
                    "IN_TEXT";
            case INVISIBLE_CIT ->
                    "EMPTY";
            default ->
                    type.name();
        };

        assertEquals(new JabRefMarkData(keys, uid, type), MarkCodec.decode(names + " " + wireType + " " + uid));
    }

    @ParameterizedTest
    @MethodSource("markCases")
    void everyFormerCidMarkIsRejectedRatherThanMigrated(String legacyName, List<String> keys, String uid, CitationType type) {
        assertThrows(MarkCodecException.class, () -> MarkCodec.decode(legacyName));
        assertFalse(MarkCodec.isCanonicalMarkName(legacyName));
        assertEquals(Optional.empty(), ZoteroReferenceMark.parse(legacyName));
    }

    private static Stream<Arguments> markCases() {
        return Stream.of(
                Arguments.of("JABREF_key1 CID_12345 uniqueId1", List.of("key1"), "uniqueId1", CitationType.AUTHORYEAR_PAR),
                Arguments.of("JABREF_key2 CID_67890 uniqueId2 NORMAL", List.of("key2"), "uniqueId2", CitationType.AUTHORYEAR_PAR),
                Arguments.of("JABREF_key3 CID_54321, JABREF_key4 CID_98765 uniqueId3 NORMAL", List.of("key3", "key4"), "uniqueId3", CitationType.AUTHORYEAR_PAR),
                Arguments.of("JABREF_key5 CID_11111, JABREF_key6 CID_22222, JABREF_key7 CID_33333 uniqueId4 NORMAL", List.of("key5", "key6", "key7"), "uniqueId4", CitationType.AUTHORYEAR_PAR),
                Arguments.of("JABREF_willberg-forssman:1997b CID_5 yov3b0su NORMAL", List.of("willberg-forssman:1997b"), "yov3b0su", CitationType.AUTHORYEAR_PAR),
                Arguments.of("JABREF_PGF/TikZTeam2023 CID_8 kyu75a4s NORMAL", List.of("PGF/TikZTeam2023"), "kyu75a4s", CitationType.AUTHORYEAR_PAR),
                Arguments.of("JABREF_Ð¢Ñ‹2025 CID_1 abc123 NORMAL", List.of("Ð¢Ñ‹2025"), "abc123", CitationType.AUTHORYEAR_PAR),
                Arguments.of("JABREF_Ð¯2025 CID_2 def456 NORMAL", List.of("Ð¯2025"), "def456", CitationType.AUTHORYEAR_PAR),
                Arguments.of("JABREF_DeutscherParitaetischerWohlfahrtsverbandâ€“Gesamtverband2016 CID_3 kku224 NORMAL", List.of("DeutscherParitaetischerWohlfahrtsverbandâ€“Gesamtverband2016"), "kku224", CitationType.AUTHORYEAR_PAR),
                Arguments.of("JABREF_Emâ€”dash CID_2 iijhy4 NORMAL", List.of("Emâ€”dash"), "iijhy4", CitationType.AUTHORYEAR_PAR),
                Arguments.of("JABREF_Keen_2011 CID_100 cite NORMAL", List.of("Keen_2011"), "cite", CitationType.AUTHORYEAR_PAR),
                Arguments.of("JABREF_legacyKey CID_42 legacyUniqueId", List.of("legacyKey"), "legacyUniqueId", CitationType.AUTHORYEAR_PAR),
                Arguments.of("JABREF_key8 CID_7 uniqueId8 IN_TEXT", List.of("key8"), "uniqueId8", CitationType.AUTHORYEAR_INTEXT),
                Arguments.of("JABREF_key9 CID_8 uniqueId9 EMPTY", List.of("key9"), "uniqueId9", CitationType.INVISIBLE_CIT));
    }

    @ParameterizedTest
    @MethodSource("recognitionCases")
    void storageModesRecognizeOnlyTheirOwnFormats(String name, boolean jabref, boolean zotero) {
        assertEquals(jabref, MarkCodec.isCanonicalMarkName(name));
        assertEquals(zotero, ZoteroReferenceMark.isZoteroReferenceMarkName(name));
    }

    private static Stream<Arguments> recognitionCases() {
        return Stream.of(
                Arguments.of("JABREF_Smith2000 CID_12345 uniqueId1 NORMAL", false, false),
                Arguments.of("JABREF_John2026 CID_7 uniqueId8 IN_TEXT", false, false),
                Arguments.of("JABREF_Smith2000 NORMAL uniqueId1", true, false),
                Arguments.of("ZOTERO_ITEM CSL_CITATION {\"citationItems\":[]} testZotero1234", false, true),
                Arguments.of("JR_cite0_1_key1", false, false),
                Arguments.of("Unrelated citations", false, false));
    }
}
