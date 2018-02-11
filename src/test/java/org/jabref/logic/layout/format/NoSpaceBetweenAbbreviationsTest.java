package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class NoSpaceBetweenAbbreviationsTest {

    @Test
    public void testFormat() {
        LayoutFormatter f = new NoSpaceBetweenAbbreviations();
        assertEquals("", f.format(""));
        assertEquals("John Meier", f.format("John Meier"));
        assertEquals("J.F. Kennedy", f.format("J. F. Kennedy"));
        assertEquals("J.R.R. Tolkien", f.format("J. R. R. Tolkien"));
        assertEquals("J.R.R. Tolkien and J.F. Kennedy", f.format("J. R. R. Tolkien and J. F. Kennedy"));
    }

}
