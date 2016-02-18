package net.sf.jabref.model.entry;

import java.util.List;

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

    @Test
    public void getSeparatedKeywords() {
        String keywords = "w1, w2a w2b, w3";
        List<String> separatedKeywords = EntryUtil.getSeparatedKeywords(keywords);
        String[] expected = new String[] {"w1", "w2a w2b", "w3"};
        Assert.assertArrayEquals(expected, separatedKeywords.toArray());
    }


}
