package org.jabref.logic.bibtex;

import java.util.Collections;

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
    void doesNotUnifyLineBreaks() {
        String original = "I\r\nunify\nline\rbreaks.";
        String processed = parser.format(new StringBuilder(original), StandardField.ABSTRACT);

        // Normalization is done at org.jabref.logic.exporter.BibWriter, so no need to normalize here
        assertEquals(original, processed);
    }

    @Test
    void retainsWhitespaceForMultiLineFields() {
        String original = "I\nkeep\nline\nbreaks\nand\n\ttabs.";

        String abstrakt = parser.format(new StringBuilder(original), StandardField.ABSTRACT);
        String review = parser.format(new StringBuilder(original), StandardField.REVIEW);

        assertEquals(original, abstrakt);
        assertEquals(original, review);
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
