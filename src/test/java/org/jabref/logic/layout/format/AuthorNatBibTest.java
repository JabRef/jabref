package org.jabref.logic.layout.format;

import org.junit.Assert;
import org.junit.Test;

public class AuthorNatBibTest {

    /**
     * Test method for
     * {@link org.jabref.logic.layout.format.AuthorNatBib#format(java.lang.String)}.
     */
    @Test
    public void testFormatThreeAuthors() {
        Assert.assertEquals("von Neumann et al.",
                new AuthorNatBib().format("von Neumann,,John and John Smith and Black Brown, Jr, Peter"));
    }

    /**
     * Test method for
     * {@link org.jabref.logic.layout.format.AuthorLF_FF#format(java.lang.String)}.
     */
    @Test
    public void testFormatTwoAuthors() {
        Assert.assertEquals("von Neumann and Smith", new AuthorNatBib().format("von Neumann,,John and John Smith"));
    }

}
