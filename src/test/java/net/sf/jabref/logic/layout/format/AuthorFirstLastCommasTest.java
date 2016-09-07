package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

import org.junit.Assert;
import org.junit.Test;

public class AuthorFirstLastCommasTest {

    /**
     * Test method for {@link net.sf.jabref.logic.layout.format.AuthorFirstLastCommas#format(java.lang.String)}.
     */
    @Test
    public void testFormat() {
        LayoutFormatter a = new AuthorFirstLastCommas();

        // Empty case
        Assert.assertEquals("", a.format(""));

        // Single Names
        Assert.assertEquals("Van Something Someone", a.format("Someone, Van Something"));

        // Two names
        Assert.assertEquals("John von Neumann and Peter Black Brown", a
                .format("John von Neumann and Peter Black Brown"));

        // Three names
        Assert.assertEquals("John von Neumann, John Smith and Peter Black Brown", a
                .format("von Neumann, John and Smith, John and Black Brown, Peter"));

        Assert.assertEquals("John von Neumann, John Smith and Peter Black Brown", a
                .format("John von Neumann and John Smith and Black Brown, Peter"));
    }

}
