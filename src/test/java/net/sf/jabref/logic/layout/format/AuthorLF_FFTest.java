package net.sf.jabref.logic.layout.format;

import org.junit.Assert;
import org.junit.Test;

public class AuthorLF_FFTest {

    /**
     * Test method for
     * {@link net.sf.jabref.logic.layout.format.AuthorLF_FF#format(java.lang.String)}.
     */
    @Test
    public void testFormat() {
        Assert.assertEquals("von Neumann, John and John Smith and Peter Black Brown, Jr",
                new AuthorLF_FF()
                        .format("von Neumann,,John and John Smith and Black Brown, Jr, Peter"));
    }

}
