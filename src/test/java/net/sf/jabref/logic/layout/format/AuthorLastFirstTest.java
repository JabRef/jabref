package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

import org.junit.Assert;
import org.junit.Test;

public class AuthorLastFirstTest {

    @Test
    public void testFormat() {
        LayoutFormatter a = new AuthorLastFirst();

        // Empty case
        Assert.assertEquals("", a.format(""));

        // Single Names
        Assert.assertEquals("Someone, Van Something", a.format("Van Something Someone"));

        // Two names
        Assert.assertEquals("von Neumann, John and Black Brown, Peter", a
                .format("John von Neumann and Black Brown, Peter"));

        // Three names
        Assert.assertEquals("von Neumann, John and Smith, John and Black Brown, Peter", a
                .format("von Neumann, John and Smith, John and Black Brown, Peter"));

        Assert.assertEquals("von Neumann, John and Smith, John and Black Brown, Peter", a
                .format("John von Neumann and John Smith and Black Brown, Peter"));
    }

}
