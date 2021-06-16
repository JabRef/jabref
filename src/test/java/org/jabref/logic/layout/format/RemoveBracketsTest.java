package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RemoveBracketsTest {
    private LayoutFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new RemoveBrackets();
    }

    @Test
    public void bracePairCorrectlyRemoved() throws Exception {
        assertEquals("some text", formatter.format("{some text}"));
    }

    @Test
    public void singleOpeningBraceCorrectlyRemoved() throws Exception {
        assertEquals("some text", formatter.format("{some text"));
    }

    @Test
    public void singleClosingBraceCorrectlyRemoved() throws Exception {
        assertEquals("some text", formatter.format("some text}"));
    }

    @Test
    public void bracePairWithEscapedBackslashCorrectlyRemoved() throws Exception {
        assertEquals("\\some text\\", formatter.format("\\{some text\\}"));
    }

    @Test
    public void withoutBracketsUnmodified() throws Exception {
        assertEquals("some text", formatter.format("some text"));
    }
}
