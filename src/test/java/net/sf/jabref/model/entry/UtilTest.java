package net.sf.jabref.model.entry;

import org.junit.Assert;
import org.junit.Test;

public class UtilTest {

    @Test
    public void testNCase() {
        Assert.assertEquals("", Util.capitalizeFirst(""));
        Assert.assertEquals("Hello world", Util.capitalizeFirst("Hello World"));
        Assert.assertEquals("A", Util.capitalizeFirst("a"));
        Assert.assertEquals("Aa", Util.capitalizeFirst("AA"));
    }
}
