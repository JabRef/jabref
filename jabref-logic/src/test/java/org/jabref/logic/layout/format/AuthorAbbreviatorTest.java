package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Is the save as the AuthorLastFirstAbbreviator.
 */
public class AuthorAbbreviatorTest {

    @Test
    public void testFormat() {

        LayoutFormatter a = new AuthorLastFirstAbbreviator();
        LayoutFormatter b = new AuthorAbbreviator();

        assertEquals(b.format(""), a.format(""));
        assertEquals(b.format("Someone, Van Something"), a.format("Someone, Van Something"));
        assertEquals(b.format("Smith, John"), a.format("Smith, John"));
        assertEquals(b.format("von Neumann, John and Smith, John and Black Brown, Peter"), a
                .format("von Neumann, John and Smith, John and Black Brown, Peter"));

    }

}
