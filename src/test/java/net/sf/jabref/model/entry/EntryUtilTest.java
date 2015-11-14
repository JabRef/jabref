package net.sf.jabref.model.entry;

import org.junit.Assert;
import org.junit.Test;

public class EntryUtilTest {

    @Test
    public void testNCase() {
        Assert.assertEquals("", EntryUtil.capitalizeFirst(""));
        Assert.assertEquals("Hello world", EntryUtil.capitalizeFirst("Hello World"));
        Assert.assertEquals("A", EntryUtil.capitalizeFirst("a"));
        Assert.assertEquals("Aa", EntryUtil.capitalizeFirst("AA"));
    }
}
