package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

import org.junit.Assert;
import org.junit.Test;

public class AuthorFirstAbbrLastCommasTest {

    @Test
    public void testFormat() {
        LayoutFormatter a = new AuthorFirstAbbrLastCommas();

        // Empty case
        Assert.assertEquals("", a.format(""));

        // Single Names
        Assert.assertEquals("V. S. Someone", a.format("Someone, Van Something"));

        // Two names
        Assert.assertEquals("J. von Neumann and P. Black Brown", a
                .format("John von Neumann and Black Brown, Peter"));

        // Three names
        Assert.assertEquals("J. von Neumann, J. Smith and P. Black Brown", a
                .format("von Neumann, John and Smith, John and Black Brown, Peter"));

        Assert.assertEquals("J. von Neumann, J. Smith and P. Black Brown", a
                .format("John von Neumann and John Smith and Black Brown, Peter"));
    }

}
