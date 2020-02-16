package org.jabref.logic.bibtex;

import java.util.Collections;

import org.jabref.logic.util.OS;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FieldContentFormatterTest {

    private FieldContentFormatter parser;

    @BeforeEach
    void setUp() throws Exception {
        FieldContentFormatterPreferences preferences = new FieldContentFormatterPreferences(Collections.emptyList());
        parser = new FieldContentFormatter(preferences);
    }

    @Test
    void unifiesLineBreaks() {
        String original = "I\r\nunify\nline\rbreaks.";
        String expected = "I\nunify\nline\nbreaks.".replace("\n", OS.NEWLINE);
        String processed = parser.format(new StringBuilder(original), StandardField.ABSTRACT);

        assertEquals(expected, processed);
    }

    @Test
    void retainsWhitespaceForMultiLineFields() {
        String original = "I\nkeep\nline\nbreaks\nand\n\ttabs.";
        String formatted = original.replace("\n", OS.NEWLINE);

        String abstrakt = parser.format(new StringBuilder(original), StandardField.ABSTRACT);
        String review = parser.format(new StringBuilder(original), StandardField.REVIEW);

        assertEquals(formatted, abstrakt);
        assertEquals(formatted, review);
    }

    @Test
    void removeWhitespaceFromNonMultiLineFields() {
        String original = "I\nshould\nnot\ninclude\nadditional\nwhitespaces  \nor\n\ttabs.";
        String expected = "I should not include additional whitespaces or tabs.";

        String abstrakt = parser.format(new StringBuilder(original), StandardField.TITLE);
        String any = parser.format(new StringBuilder(original), new UnknownField("anyotherfield"));

        assertEquals(expected, abstrakt);
        assertEquals(expected, any);
    }
}
