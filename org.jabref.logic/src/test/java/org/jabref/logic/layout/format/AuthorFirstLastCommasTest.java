package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class AuthorFirstLastCommasTest {

    /**
     * Test method for {@link org.jabref.logic.layout.format.AuthorFirstLastCommas#format(java.lang.String)}.
     */
    @Test
    public void testFormat() {
        LayoutFormatter a = new AuthorFirstLastCommas();

        // Empty case
        assertEquals("", a.format(""));

        // Single Names
        assertEquals("Van Something Someone", a.format("Someone, Van Something"));

        // Two names
        assertEquals("John von Neumann and Peter Black Brown", a
                .format("John von Neumann and Peter Black Brown"));

        // Three names
        assertEquals("John von Neumann, John Smith and Peter Black Brown", a
                .format("von Neumann, John and Smith, John and Black Brown, Peter"));

        assertEquals("John von Neumann, John Smith and Peter Black Brown", a
                .format("John von Neumann and John Smith and Black Brown, Peter"));
    }

}
