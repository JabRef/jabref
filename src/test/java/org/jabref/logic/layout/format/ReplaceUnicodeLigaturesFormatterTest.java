package org.jabref.logic.layout.format;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReplaceUnicodeLigaturesFormatterTest {

    private ReplaceUnicodeLigaturesFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new ReplaceUnicodeLigaturesFormatter();
    }

    @Test
    void plainFormat() {
        assertEquals("lorem ipsum", formatter.format("lorem ipsum"));
    }

    @Test
    void singleLigatures() {
        assertEquals("AA", formatter.format("\uA732"));
        assertEquals("fi", formatter.format("ﬁ"));
        assertEquals("et", formatter.format("\uD83D\uDE70"));
    }

    @Test
    void ligatureSequence() {
        assertEquals("aefffflstue", formatter.format("æﬀﬄﬆᵫ"));
    }

    @Test
    void sampleInput() {
        assertEquals("AEneas", formatter.format("Æneas"));
    }
}
