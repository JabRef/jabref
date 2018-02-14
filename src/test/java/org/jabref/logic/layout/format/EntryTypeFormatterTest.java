package org.jabref.logic.layout.format;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EntryTypeFormatterTest {

    private EntryTypeFormatter formatter;


    @BeforeEach
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
