package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.Assert;
import org.junit.Test;

public class AuthorAndsCommaReplacerTest {

    /**
     * Test method for
     * {@link org.jabref.logic.layout.format.AuthorAndsCommaReplacer#format(java.lang.String)}.
     */
    @Test
    public void testFormat() {

        LayoutFormatter a = new AuthorAndsCommaReplacer();

        // Empty case
        Assert.assertEquals("", a.format(""));

        // Single Names don't change
        Assert.assertEquals("Someone, Van Something", a.format("Someone, Van Something"));

        // Two names just an &
        Assert.assertEquals("John von Neumann & Peter Black Brown",
                a.format("John von Neumann and Peter Black Brown"));

        // Three names put a comma:
        Assert.assertEquals("von Neumann, John, Smith, John & Black Brown, Peter",
                a.format("von Neumann, John and Smith, John and Black Brown, Peter"));
    }
}
