package org.jabref.logic.layout.format;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class AuthorLF_FFTest {

    /**
     * Test method for
     * {@link org.jabref.logic.layout.format.AuthorLF_FF#format(java.lang.String)}.
     */
    @Test
    public void testFormat() {
        assertEquals("von Neumann, John and John Smith and Peter Black Brown, Jr",
                new AuthorLF_FF()
                        .format("von Neumann,,John and John Smith and Black Brown, Jr, Peter"));
    }

}
