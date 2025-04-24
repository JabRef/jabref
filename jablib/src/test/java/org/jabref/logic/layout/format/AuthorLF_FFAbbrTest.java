package org.jabref.logic.layout.format;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorLF_FFAbbrTest {

    /**
     * Test method for {@link org.jabref.logic.layout.format.AuthorLF_FFAbbr#format(java.lang.String)}.
     */
    @Test
    void format() {
        assertEquals("von Neumann, J. and J. Smith and P. Black Brown, Jr",
                new AuthorLF_FFAbbr()
                        .format("von Neumann,,John and John Smith and Black Brown, Jr, Peter"));
    }
}
