package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemoveBracketsTest {
    private LayoutFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new RemoveBrackets();
    }

    @Test
    void bracePairCorrectlyRemoved() throws Exception {
        assertEquals("some text", formatter.format("{some text}"));
    }

    @Test
    void singleOpeningBraceCorrectlyRemoved() throws Exception {
        assertEquals("some text", formatter.format("{some text"));
    }

    @Test
    void singleClosingBraceCorrectlyRemoved() throws Exception {
        assertEquals("some text", formatter.format("some text}"));
    }

    @Test
    void bracePairWithEscapedBackslashCorrectlyRemoved() throws Exception {
        assertEquals("\\some text\\", formatter.format("\\{some text\\}"));
    }

    @Test
    void withoutBracketsUnmodified() throws Exception {
        assertEquals("some text", formatter.format("some text"));
    }
}
