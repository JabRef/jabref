package net.sf.jabref.logic.layout.format;

import org.junit.Assert;
import org.junit.Test;

public class AuthorFirstFirstCommasTest {

    /**
     * Test method for
     * {@link net.sf.jabref.logic.layout.format.AuthorFirstFirstCommas#format(java.lang.String)}.
     */
    @Test
    public void testFormat() {
        Assert.assertEquals("John von Neumann, John Smith and Peter Black Brown, Jr",
                new AuthorFirstFirstCommas()
                        .format("von Neumann,,John and John Smith and Black Brown, Jr, Peter"));
    }

}
