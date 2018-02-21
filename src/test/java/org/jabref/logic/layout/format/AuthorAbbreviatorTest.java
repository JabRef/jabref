package org.jabref.logic.layout.format;

import org.jabref.logic.layout.LayoutFormatter;

import org.junit.Assert;
import org.junit.Test;

/**
 * Is the save as the AuthorLastFirstAbbreviator.
 */
public class AuthorAbbreviatorTest {

    @Test
    public void testFormat() {

        LayoutFormatter a = new AuthorLastFirstAbbreviator();
        LayoutFormatter b = new AuthorAbbreviator();

        Assert.assertEquals(b.format(""), a.format(""));
        Assert.assertEquals(b.format("Someone, Van Something"), a.format("Someone, Van Something"));
        Assert.assertEquals(b.format("Smith, John"), a.format("Smith, John"));
        Assert.assertEquals(b.format("von Neumann, John and Smith, John and Black Brown, Peter"), a
                .format("von Neumann, John and Smith, John and Black Brown, Peter"));

    }

}
