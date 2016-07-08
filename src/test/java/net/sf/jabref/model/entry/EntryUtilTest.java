package net.sf.jabref.model.entry;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

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
        Set<String> separatedKeywords = EntryUtil.getSeparatedKeywords(keywords);
        Assert.assertEquals(new LinkedHashSet<>(Arrays.asList("w1", "w2a w2b", "w3")), separatedKeywords);
    }


}
