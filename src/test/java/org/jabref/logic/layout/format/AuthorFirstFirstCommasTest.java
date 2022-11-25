package org.jabref.logic.layout.format;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthorFirstFirstCommasTest {

    /**
     * Test method for {@link org.jabref.logic.layout.format.AuthorFirstFirstCommas#format(java.lang.String)}.
     */
    @Test
    public void testFormat() {
        assertEquals("John von Neumann, John Smith and Peter Black Brown, Jr",
                new AuthorFirstFirstCommas()
                        .format("von Neumann,,John and John Smith and Black Brown, Jr, Peter"));
    }
}
