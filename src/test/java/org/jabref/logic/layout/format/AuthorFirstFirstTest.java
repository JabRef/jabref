package org.jabref.logic.layout.format;

import org.junit.Assert;
import org.junit.Test;

public class AuthorFirstFirstTest {

    /**
     * Test method for
     * {@link org.jabref.logic.layout.format.AuthorFirstFirst#format(java.lang.String)}.
     */
    @Test
    public void testFormat() {
        Assert.assertEquals("John von Neumann and John Smith and Peter Black Brown, Jr",
                new AuthorFirstFirst()
                        .format("von Neumann,,John and John Smith and Black Brown, Jr, Peter"));
    }

}
