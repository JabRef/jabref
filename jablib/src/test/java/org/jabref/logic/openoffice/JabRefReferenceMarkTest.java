package org.jabref.logic.openoffice;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.openoffice.oocsltext.CSLCitationType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class JabRefReferenceMarkTest {

    @Test
    void buildSingleReferenceMark() {
        String referenceMarkName = JabRefReferenceMark.buildReferenceMarkName(
                List.of("key1"),
                List.of(12345),
                "uniqueId1",
                CSLCitationType.NORMAL);

        assertEquals("JABREF_key1 CID_12345 uniqueId1 NORMAL", referenceMarkName);
    }

    @Test
    void buildGroupedReferenceMark() {
        String referenceMarkName = JabRefReferenceMark.buildReferenceMarkName(
                List.of("key3", "key4"),
                List.of(54321, 98765),
                "uniqueId3",
                CSLCitationType.IN_TEXT);

        assertEquals("JABREF_key3 CID_54321, JABREF_key4 CID_98765 uniqueId3 IN_TEXT", referenceMarkName);
    }

    @Test
    void parseReferenceMarkWithoutCitationTypeMarker() {
        String referenceMarkName = "JABREF_legacyKey CID_42 legacyUniqueId";
        Optional<JabRefReferenceMark> markData = JabRefReferenceMark.parse(referenceMarkName);

        assertEquals(Optional.of(new JabRefReferenceMark(
                referenceMarkName,
                List.of("legacyKey"),
                List.of(42),
                "legacyUniqueId",
                CSLCitationType.NORMAL)), markData);
    }

    @Test
    void parseGroupedReferenceMarkWithCitationTypeMarker() {
        String referenceMarkName = "JABREF_key3 CID_54321, JABREF_key4 CID_98765 uniqueId3 IN_TEXT";
        Optional<JabRefReferenceMark> markData = JabRefReferenceMark.parse(referenceMarkName);

        assertEquals(Optional.of(new JabRefReferenceMark(
                referenceMarkName,
                List.of("key3", "key4"),
                List.of(54321, 98765),
                "uniqueId3",
                CSLCitationType.IN_TEXT)), markData);
    }

    @Test
    void parseUnrecognizedName() {
        Optional<JabRefReferenceMark> markData = JabRefReferenceMark.parse("Unrelated citations");

        assertEquals(Optional.empty(), markData);
    }
}
