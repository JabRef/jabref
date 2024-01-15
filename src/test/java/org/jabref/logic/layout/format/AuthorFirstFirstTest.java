package org.jabref.logic.layout.format;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorFirstFirstTest {

    /**
     * Test method for {@link org.jabref.logic.layout.format.AuthorFirstFirst#format(java.lang.String)}.
     */
    @Test
    void format() {
        assertEquals("John von Neumann and John Smith and Peter Black Brown, Jr",
                new AuthorFirstFirst()
                        .format("von Neumann,,John and John Smith and Black Brown, Jr, Peter"));
    }
}
