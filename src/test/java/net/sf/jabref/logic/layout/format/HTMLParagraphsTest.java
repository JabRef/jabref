package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.LayoutFormatter;

import org.junit.Assert;
import org.junit.Test;

public class HTMLParagraphsTest {

    @Test
    public void testFormat() {

        LayoutFormatter f = new HTMLParagraphs();

        Assert.assertEquals("", f.format(""));
        Assert.assertEquals("<p>\nHello\n</p>", f.format("Hello"));
        Assert.assertEquals("<p>\nHello\nWorld\n</p>", f.format("Hello\nWorld"));
        Assert.assertEquals("<p>\nHello World\n</p>\n<p>\nWhat a lovely day\n</p>", f.format("Hello World\n   \nWhat a lovely day\n"));
        Assert.assertEquals("<p>\nHello World\n</p>\n<p>\nCould not be any better\n</p>\n<p>\nWhat a lovely day\n</p>", f.format("Hello World\n \n\nCould not be any better\n\nWhat a lovely day\n"));

    }

}
