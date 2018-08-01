package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class HTMLParagraphsTest {

    @Test
    public void testFormat() {

        LayoutFormatter f = new HTMLParagraphs();

        assertEquals("", f.format(""));
        assertEquals("<p>\nHello\n</p>", f.format("Hello"));
        assertEquals("<p>\nHello\nWorld\n</p>", f.format("Hello\nWorld"));
        assertEquals("<p>\nHello World\n</p>\n<p>\nWhat a lovely day\n</p>", f.format("Hello World\n   \nWhat a lovely day\n"));
        assertEquals("<p>\nHello World\n</p>\n<p>\nCould not be any better\n</p>\n<p>\nWhat a lovely day\n</p>", f.format("Hello World\n \n\nCould not be any better\n\nWhat a lovely day\n"));

    }

}
