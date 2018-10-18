package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthorAndsReplacerTest {

    /**
     * Test method for {@link org.jabref.logic.layout.format.AuthorAndsReplacer#format(java.lang.String)}.
     */
    @Test
    public void testFormat() {
        LayoutFormatter a = new AuthorAndsReplacer();

        // Empty case
        assertEquals("", a.format(""));

        // Single Names don't change
        assertEquals("Someone, Van Something", a.format("Someone, Van Something"));

        // Two names just an &
        assertEquals("John Smith & Black Brown, Peter", a
                .format("John Smith and Black Brown, Peter"));

        // Three names put a comma:
        assertEquals("von Neumann, John; Smith, John & Black Brown, Peter", a
                .format("von Neumann, John and Smith, John and Black Brown, Peter"));

        assertEquals("John von Neumann; John Smith & Peter Black Brown", a
                .format("John von Neumann and John Smith and Peter Black Brown"));
    }
}
