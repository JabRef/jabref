package net.sf.jabref.export.layout.format;

import junit.framework.Assert;
import net.sf.jabref.export.layout.LayoutFormatter;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RemoveBracketsAddCommaTest {
    private LayoutFormatter formatter;

    @Before
    public void setup() {
        formatter = new RemoveBracketsAddComma();
    }

    @Test
    public void testFormat() throws Exception {
        Assert.assertEquals("some text,", formatter.format("{some text}"));
        Assert.assertEquals("some text", formatter.format("{some text"));
        Assert.assertEquals("some text,", formatter.format("some text}"));
    }
}