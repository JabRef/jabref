package org.jabref.logic.layout.format;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class AuthorFirstFirstCommasTest {

    /**
     * Test method for
     * {@link org.jabref.logic.layout.format.AuthorFirstFirstCommas#format(java.lang.String)}.
     */
    @Test
    public void testFormat() {
        assertEquals("John von Neumann, John Smith and Peter Black Brown, Jr",
                new AuthorFirstFirstCommas()
                        .format("von Neumann,,John and John Smith and Black Brown, Jr, Peter"));
    }

}
