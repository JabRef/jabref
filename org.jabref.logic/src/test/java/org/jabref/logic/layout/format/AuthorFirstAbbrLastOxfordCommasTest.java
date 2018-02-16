package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class AuthorFirstAbbrLastOxfordCommasTest {

    /**
     * Test method for {@link org.jabref.logic.layout.format.AuthorFirstAbbrLastOxfordCommas#format(java.lang.String)}.
     */
    @Test
    public void testFormat() {
        LayoutFormatter a = new AuthorFirstAbbrLastOxfordCommas();

        // Empty case
        assertEquals("", a.format(""));

        // Single Names
        assertEquals("V. S. Someone", a.format("Someone, Van Something"));

        // Two names
        assertEquals("J. von Neumann and P. Black Brown", a
                .format("John von Neumann and Black Brown, Peter"));

        // Three names
        assertEquals("J. von Neumann, J. Smith, and P. Black Brown", a
                .format("von Neumann, John and Smith, John and Black Brown, Peter"));

        assertEquals("J. von Neumann, J. Smith, and P. Black Brown", a
                .format("John von Neumann and John Smith and Black Brown, Peter"));
    }

}
