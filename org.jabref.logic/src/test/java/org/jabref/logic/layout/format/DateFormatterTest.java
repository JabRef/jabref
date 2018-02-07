package org.jabref.logic.layout.format;

import org.jabref.logic.layout.ParamLayoutFormatter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DateFormatterTest {

    private ParamLayoutFormatter formatter;

    @Before
    public void setUp() {
        formatter = new DateFormatter();
    }

    @Test
    public void testDefaultFormat() {
        Assert.assertEquals("2016-07-15", formatter.format("2016-07-15"));
    }

    @Test
    public void testRequestedFormat() {
        formatter.setArgument("MM/yyyy");
        Assert.assertEquals("07/2016", formatter.format("2016-07-15"));
    }

}
