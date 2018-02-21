package org.jabref.logic.layout.format;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EntryTypeFormatterTest {

    private EntryTypeFormatter formatter;


    @Before
    public void setUp() {
        formatter = new EntryTypeFormatter();
    }

    @Test
    public void testCorrectFormatArticle() {
        assertEquals("Article", formatter.format("article"));
    }

    @Test
    public void testCorrectFormatInBook() {
        assertEquals("InBook", formatter.format("inbook"));
    }

    @Test
    public void testIncorrectTypeAarticle() {
        assertEquals("Aarticle", formatter.format("aarticle"));
    }

}
