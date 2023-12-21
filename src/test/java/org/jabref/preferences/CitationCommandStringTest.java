package org.jabref.preferences;

import org.jabref.logic.push.CitationCommandString;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CitationCommandStringTest {

    @Test
    void testToString() {
        assertEquals("\\cite{key1,key2}", new CitationCommandString("\\cite{", ",", "}").toString());
    }

    @Test
    void from() {
        CitationCommandString expected = new CitationCommandString("\\cite{", ",", "}");
        assertEquals(expected, CitationCommandString.from("\\cite{key1,key2}"));
    }
}
