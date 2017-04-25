package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.Assert;
import org.junit.Test;

public class AuthorAndToSemicolonReplacerTest {

    /**
     * Test method for
     * {@link org.jabref.logic.layout.format.AuthorAndToSemicolonReplacer#format(java.lang.String)}.
     */
    @Test
    public void testFormat() {
        LayoutFormatter a = new AuthorAndToSemicolonReplacer();

        // Empty case
        Assert.assertEquals("", a.format(""));

        // Single Names don't change
        Assert.assertEquals("Someone, Van Something", a.format("Someone, Van Something"));

        // Two names just one semicolon
        Assert.assertEquals("John Smith; Black Brown, Peter", a
                .format("John Smith and Black Brown, Peter"));

        // Three names put two semicolons
        Assert.assertEquals("von Neumann, John; Smith, John; Black Brown, Peter", a
                .format("von Neumann, John and Smith, John and Black Brown, Peter"));

        Assert.assertEquals("John von Neumann; John Smith; Peter Black Brown", a
                .format("John von Neumann and John Smith and Peter Black Brown"));
    }
}
