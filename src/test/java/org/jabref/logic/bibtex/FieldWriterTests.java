package org.jabref.logic.bibtex;

import java.util.Collections;

import org.jabref.logic.util.OS;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class FieldWriterTests {

    private FieldWriter writer;

    @BeforeEach
    void setUp() {
        this.writer = new FieldWriter(mock(FieldWriterPreferences.class, Answers.RETURNS_DEEP_STUBS));
    }

    @Test
    void normalizeNewlineInAbstractField() throws Exception {
        String text = "lorem" + OS.NEWLINE + " ipsum lorem ipsum\nlorem ipsum \rlorem ipsum\r\ntest";

        String expected = "{" + "lorem" + OS.NEWLINE + " ipsum lorem ipsum" + OS.NEWLINE
                + "lorem ipsum "
                + OS.NEWLINE + "lorem ipsum"
                + OS.NEWLINE + "test" + "}";

        String result = writer.write(StandardField.ABSTRACT, text);

        assertEquals(expected, result);
    }

    @Test
    void preserveNewlineInAbstractField() throws Exception {
        String text = "lorem ipsum lorem ipsum" + OS.NEWLINE + "lorem ipsum lorem ipsum";

        String result = writer.write(StandardField.ABSTRACT, text);
        String expected = "{" + text + "}";

        assertEquals(expected, result);
    }

    @Test
    void preserveMultipleNewlinesInAbstractField() throws Exception {
        String text = "lorem ipsum lorem ipsum" + OS.NEWLINE + OS.NEWLINE + "lorem ipsum lorem ipsum";

        String result = writer.write(StandardField.ABSTRACT, text);
        String expected = "{" + text + "}";

        assertEquals(expected, result);
    }

    @Test
    void preserveNewlineInReviewField() throws Exception {
        String text = "lorem ipsum lorem ipsum" + OS.NEWLINE + "lorem ipsum lorem ipsum";

        String result = writer.write(StandardField.REVIEW, text);
        String expected = "{" + text + "}";

        assertEquals(expected, result);
    }

    @Test
    void removeWhitespaceFromNonMultiLineFields() throws Exception {
        String original = "I\nshould\nnot\ninclude\nadditional\nwhitespaces  \nor\n\ttabs.";
        String expected = "{I should not include additional whitespaces or tabs.}";

        String title = writer.write(StandardField.TITLE, original);
        String any = writer.write(new UnknownField("anyotherfield"), original);

        assertEquals(expected, title);
        assertEquals(expected, any);
    }

    @Test
    void reportUnbalancedBracing() throws Exception {
        String unbalanced = "{";

        assertThrows(InvalidFieldValueException.class, () -> writer.write(new UnknownField("anyfield"), unbalanced));
    }

    @Test
    void reportUnbalancedBracingWithEscapedBraces() throws Exception {
        String unbalanced = "{\\}";

        assertThrows(InvalidFieldValueException.class, () -> writer.write(new UnknownField("anyfield"), unbalanced));
    }

    @Test
    void tolerateBalancedBrace() throws Exception {
        String text = "Incorporating evolutionary {Measures into Conservation Prioritization}";

        assertEquals("{" + text + "}", writer.write(new UnknownField("anyfield"), text));
    }

    @Test
    void tolerateEscapeCharacters() throws Exception {
        String text = "Incorporating {\\O}evolutionary {Measures into Conservation Prioritization}";

        assertEquals("{" + text + "}", writer.write(new UnknownField("anyfield"), text));
    }

    @Test
    void hashEnclosedWordsGetRealStringsInMonthField() throws Exception {
        String text = "#jan# - #feb#";
        assertEquals("jan #{ - } # feb", writer.write(StandardField.MONTH, text));
    }

    @Test
    void hashEnclosedWordsGetRealStringsInMonthFieldBecauseMonthIsStandardField() throws Exception {
        FieldWriterPreferences fieldWriterPreferences = new FieldWriterPreferences(
                false, Collections.emptyList(), new FieldContentFormatterPreferences());
        FieldWriter formatter = new FieldWriter(fieldWriterPreferences);
        String text = "#jan# - #feb#";
        assertEquals("jan #{ - } # feb", formatter.write(StandardField.MONTH, text));
    }
}
