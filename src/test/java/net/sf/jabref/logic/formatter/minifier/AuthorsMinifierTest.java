package net.sf.jabref.logic.formatter.minifier;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AuthorsMinifierTest {
    private AuthorsMinifier formatter;

    @Before
    public void setUp() {
        formatter = new AuthorsMinifier();
    }

    @After
    public void teardown() {
        formatter = null;
    }

    @Test
    public void returnsFormatterName() {
        Assert.assertNotNull(formatter.getName());
        Assert.assertNotEquals("", formatter.getName());
    }

    @Test
    public void minifyAuthorNames() {
        expectCorrect("Simon Harrer", "Simon Harrer");
        expectCorrect("Simon Harrer and others", "Simon Harrer and others");
        expectCorrect("Simon Harrer and Jörg Lenhard", "Simon Harrer and Jörg Lenhard");
        expectCorrect("Simon Harrer and Jörg Lenhard and Guido Wirtz", "Simon Harrer and others");
        expectCorrect("Simon Harrer and Jörg Lenhard and Guido Wirtz and others", "Simon Harrer and others");
    }

    @Test
    public void formatEmptyFields() {
        expectCorrect("", "");
        expectCorrect(null, null);
    }

    private void expectCorrect(String input, String expected) {
        Assert.assertEquals(expected, formatter.format(input));
    }
}