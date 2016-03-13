package net.sf.jabref.logic.formatter.bibtexfields;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AuthorsFormatterTestNonParamterized {

    private AuthorsFormatter formatter;

    @Before
    public void setUp() {
        formatter = new AuthorsFormatter();
    }

    @Test
    public void returnsFormatterName() {
        Assert.assertNotNull(formatter.getName());
        Assert.assertNotEquals("", formatter.getName());
    }

}