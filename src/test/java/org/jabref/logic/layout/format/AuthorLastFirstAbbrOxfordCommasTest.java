package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthorLastFirstAbbrOxfordCommasTest {

    /**
     * Test method for {@link org.jabref.logic.layout.format.AuthorLastFirstAbbrOxfordCommas#format(java.lang.String)}.
     */
    @Test
    public void testFormat() {
        LayoutFormatter a = new AuthorLastFirstAbbrOxfordCommas();

        // Empty case
        assertEquals("", a.format(""));

        // Single Names
        assertEquals("Someone, V. S.", a.format("Van Something Someone"));

        // Two names
        assertEquals("von Neumann, J. and Black Brown, P.", a
                .format("John von Neumann and Black Brown, Peter"));

        // Three names
        assertEquals("von Neumann, J., Smith, J., and Black Brown, P.", a
                .format("von Neumann, John and Smith, John and Black Brown, Peter"));

        assertEquals("von Neumann, J., Smith, J., and Black Brown, P.", a
                .format("John von Neumann and John Smith and Black Brown, Peter"));
    }
}
