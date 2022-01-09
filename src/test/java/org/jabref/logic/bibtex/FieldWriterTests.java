package org.jabref.logic.bibtex;

import java.util.List;

import org.jabref.logic.util.OS;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.strings.StringUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FieldWriterTests {

    private FieldWriter writer;

    @BeforeEach
    void setUp() {
        FieldWriterPreferences fieldWriterPreferences = new FieldWriterPreferences(true, List.of(StandardField.MONTH), new FieldContentFormatterPreferences());
        writer = new FieldWriter(fieldWriterPreferences);
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
    void keepHashSignInComment() throws Exception {
        String text = "# Changelog\n"
                      + "\n"
                      + "All notable changes to this project will be documented in this file.\n"
                      + "The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).\n"
                      + "We refer to [GitHub issues](https://github.com/JabRef/jabref/issues) by using `#NUM`.\n"
                      + "In case, there is no issue present, the pull request implementing the feature is linked.\n"
                      + "\n"
                      + "Note that this project **does not** adhere to [Semantic Versioning](http://semver.org/).\n"
                      + "\n"
                      + "## [Unreleased]";

        String writeResult = writer.write(StandardField.COMMENT, text);
        String resultWithLfAsNewLineSeparator = StringUtil.unifyLineBreaks(writeResult, "\n");
        assertEquals("{" + text + "}", resultWithLfAsNewLineSeparator);
    }
}
