package net.sf.jabref.importer.fileformat;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class FieldContentParserTest {
    FieldContentParser parser;

    @Before
    public void setUp() throws Exception {
        parser = new FieldContentParser();
    }

    @Test
    public void unifiesLineBreaks() throws Exception {
        String original = "I\r\nunify\nline\rbreaks.";
        String expected = "I\nunify\nline\nbreaks.";
        String processed = parser.format(new StringBuffer(original), "abstract").toString();

        Assert.assertEquals(expected, processed);
    }

    @Test
    public void retainsWhitespaceForMultiLineFields() throws Exception {
        String original = "I\nkeep\nline\nbreaks\nand\n\ttabs.";
        String abstrakt = parser.format(new StringBuffer(original), "abstract").toString();
        String review = parser.format(new StringBuffer(original), "review").toString();

        Assert.assertEquals(original, abstrakt);
        Assert.assertEquals(original, review);
    }

    @Test
    public void removeWhitespaceFromNonMultiLineFields() throws Exception {
        String original = "I\nshould\nnot\ninclude\nadditional\nwhitespaces  \nor\n\ttabs.";
        String expected = "I should not include additional whitespaces or tabs.";

        String abstrakt = parser.format(new StringBuffer(original), "title").toString();
        String review = parser.format(new StringBuffer(original), "doi").toString();
        String any = parser.format(new StringBuffer(original), "anyotherfield").toString();

        Assert.assertEquals(expected, abstrakt);
        Assert.assertEquals(expected, review);
        Assert.assertEquals(expected, any);
    }
}