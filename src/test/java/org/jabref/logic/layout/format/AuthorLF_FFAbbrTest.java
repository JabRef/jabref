package org.jabref.logic.layout.format;

import org.junit.Assert;
import org.junit.Test;

public class AuthorLF_FFAbbrTest {

    /**
     * Test method for
     * {@link org.jabref.logic.layout.format.AuthorLF_FFAbbr#format(java.lang.String)}.
     */
    @Test
    public void testFormat() {
        Assert.assertEquals("von Neumann, J. and J. Smith and P. Black Brown, Jr",
                new AuthorLF_FFAbbr()
                        .format("von Neumann,,John and John Smith and Black Brown, Jr, Peter"));
    }

}
