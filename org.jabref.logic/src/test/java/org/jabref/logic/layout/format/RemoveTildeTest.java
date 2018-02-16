package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RemoveTildeTest {
    private LayoutFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new RemoveTilde();
    }

    @Test
    public void testFormatString() {
        assertEquals("", formatter.format(""));
        assertEquals("simple", formatter.format("simple"));
        assertEquals(" ", formatter.format("~"));
        assertEquals("   ", formatter.format("~~~"));
        assertEquals(" \\~ ", formatter.format("~\\~~"));
        assertEquals("\\\\ ", formatter.format("\\\\~"));
        assertEquals("Doe Joe and Jane, M. and Kamp, J. A.", formatter.format("Doe Joe and Jane, M. and Kamp, J.~A."));
        assertEquals("T\\~olkien, J. R. R.", formatter.format("T\\~olkien, J.~R.~R."));
    }
}
