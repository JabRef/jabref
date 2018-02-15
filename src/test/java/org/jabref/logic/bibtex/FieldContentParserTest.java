package org.jabref.logic.bibtex;

import java.util.Collections;

import org.jabref.logic.util.OS;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FieldContentParserTest {

    private FieldContentParser parser;


    @BeforeEach
    public void setUp() throws Exception {
        FieldContentParserPreferences prefs = new FieldContentParserPreferences(Collections.emptyList());
        parser = new FieldContentParser(prefs);
    }

    @Test
    public void unifiesLineBreaks() {
        String original = "I\r\nunify\nline\rbreaks.";
        String expected = "I\nunify\nline\nbreaks.".replace("\n", OS.NEWLINE);
        String processed = parser.format(new StringBuilder(original), "abstract").toString();

        assertEquals(expected, processed);
    }

    @Test
    public void retainsWhitespaceForMultiLineFields() {
        String original = "I\nkeep\nline\nbreaks\nand\n\ttabs.";
        String formatted = original.replace("\n", OS.NEWLINE);

        String abstrakt = parser.format(new StringBuilder(original), "abstract").toString();
        String review = parser.format(new StringBuilder(original), "review").toString();

        assertEquals(formatted, abstrakt);
        assertEquals(formatted, review);
    }

    @Test
    public void removeWhitespaceFromNonMultiLineFields() {
        String original = "I\nshould\nnot\ninclude\nadditional\nwhitespaces  \nor\n\ttabs.";
        String expected = "I should not include additional whitespaces or tabs.";

        String abstrakt = parser.format(new StringBuilder(original), "title").toString();
        String any = parser.format(new StringBuilder(original), "anyotherfield").toString();

        assertEquals(expected, abstrakt);
        assertEquals(expected, any);
    }
}
