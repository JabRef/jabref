package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.Assert;
import org.junit.Test;

public class AuthorLastFirstAbbrCommasTest {

    /**
     * Test method for {@link org.jabref.logic.layout.format.AuthorLastFirstAbbrCommas#format(java.lang.String)}.
     */
    @Test
    public void testFormat() {
        LayoutFormatter a = new AuthorLastFirstAbbrCommas();

        // Empty case
        Assert.assertEquals("", a.format(""));

        // Single Names
        Assert.assertEquals("Someone, V. S.", a.format("Van Something Someone"));

        // Two names
        Assert.assertEquals("von Neumann, J. and Black Brown, P.", a
                .format("John von Neumann and Black Brown, Peter"));

        // Three names
        Assert.assertEquals("von Neumann, J., Smith, J. and Black Brown, P.", a
                .format("von Neumann, John and Smith, John and Black Brown, Peter"));

        Assert.assertEquals("von Neumann, J., Smith, J. and Black Brown, P.", a
                .format("John von Neumann and John Smith and Black Brown, Peter"));

    }

}
