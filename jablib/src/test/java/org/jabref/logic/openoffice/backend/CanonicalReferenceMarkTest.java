package org.jabref.logic.openoffice.backend;

import java.util.List;

import org.jabref.logic.openoffice.engine.JabRefMarkData;
import org.jabref.logic.openoffice.engine.MarkCodec;
import org.jabref.logic.openoffice.engine.MarkCodecException;
import org.jabref.model.openoffice.style.CitationType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@NullMarked
class CanonicalReferenceMarkTest {

    @Test
    void citationTypesRoundTripWithoutDerivedNumbersOrChangingStorageOrder() throws MarkCodecException {
        for (CitationType citationType : CitationType.values()) {
            JabRefMarkData identity = new JabRefMarkData(List.of("key2", "key1", "key2"), "stable-id", citationType);
            JabRefMarkData parsed = MarkCodec.decode(MarkCodec.encode(identity));

            assertEquals("stable-id", parsed.uniqueId());
            assertEquals(citationType, parsed.citationType());
            assertEquals(List.of("key2", "key1", "key2"), parsed.citationKeys());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"JR_cite_4_key", "JR_cite_5_key", "JR_cite_6_key", "JABREF_key CID_1 stable NORMAL"})
    void legacyCitationNamesAreRejectedRatherThanMigrated(String name) {
        assertThrows(MarkCodecException.class, () -> MarkCodec.decode(name));
    }
}
