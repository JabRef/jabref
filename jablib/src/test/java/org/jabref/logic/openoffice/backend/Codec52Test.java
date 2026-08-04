package org.jabref.logic.openoffice.backend;

import java.util.List;
import java.util.Set;

import org.jabref.model.openoffice.style.CitationType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class Codec52Test {

    @Test
    void citationTypesRoundTripInReferenceMarkNames() {
        for (CitationType citationType : CitationType.values()) {
            String markName = Codec52.getUniqueMarkName(Set.of(), List.of("key1", "key2"), citationType);
            Codec52.ParsedMarkName parsedMarkName = Codec52.parseMarkName(markName).orElseThrow();

            assertEquals("", parsedMarkName.index);
            assertEquals(citationType, parsedMarkName.citationType);
            assertEquals(List.of("key1", "key2"), parsedMarkName.citationKeys);
        }
    }

    @Test
    void parseMarkNameRecognizesExtendedCitationTypeCodes() {
        assertEquals(CitationType.AUTHORYEAR_NOPAR,
                Codec52.parseMarkName("JR_cite_4_key").orElseThrow().citationType);
        assertEquals(CitationType.AUTHOR_ONLY,
                Codec52.parseMarkName("JR_cite_5_key").orElseThrow().citationType);
        assertEquals(CitationType.YEAR_ONLY,
                Codec52.parseMarkName("JR_cite_6_key").orElseThrow().citationType);
    }
}
