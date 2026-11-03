package org.jabref.logic.openoffice.engine;

import java.util.List;

import org.jabref.model.openoffice.style.CitationType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@NullMarked
class CanonicalMarkDataTest {

    @Test
    void buildSingleReferenceMarkWithoutDerivedNumber() throws MarkCodecException {
        assertEquals("JABREF_key1 NORMAL uniqueId1", MarkCodec.encode(new JabRefMarkData(List.of("key1"), "uniqueId1", CitationType.AUTHORYEAR_PAR)));
    }

    @Test
    void buildGroupedReferenceMarkWithoutDerivedNumbers() throws MarkCodecException {
        assertEquals("JABREF_key3, JABREF_key4 IN_TEXT uniqueId3", MarkCodec.encode(new JabRefMarkData(List.of("key3", "key4"), "uniqueId3", CitationType.AUTHORYEAR_INTEXT)));
    }

    @Test
    void missingTypeIsNotDefaultedFromLegacyMark() {
        assertThrows(MarkCodecException.class, () -> MarkCodec.decode("JABREF_legacyKey CID_42 legacyUniqueId"));
    }

    @Test
    void parseGroupedReferenceMarkWithExplicitType() throws MarkCodecException {
        assertEquals(new JabRefMarkData(List.of("key3", "key4"), "uniqueId3", CitationType.AUTHORYEAR_INTEXT),
                MarkCodec.decode("JABREF_key3, JABREF_key4 IN_TEXT uniqueId3"));
    }

    @Test
    void unrelatedNamesAreRejected() {
        assertThrows(MarkCodecException.class, () -> MarkCodec.decode("Unrelated citations"));
    }
}
