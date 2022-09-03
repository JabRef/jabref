package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.formatter.bibtexfields.EscapeAmpersandsFormatter;
import org.jabref.logic.formatter.bibtexfields.EscapeDollarSignFormatter;
import org.jabref.logic.formatter.bibtexfields.EscapeUnderscoresFormatter;
import org.jabref.logic.formatter.bibtexfields.LatexCleanupFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizeMonthFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CleanupsTest {

    @Test
    void parserKeepsSaveActions() {
        List<FieldFormatterCleanup> fieldFormatterCleanups = Cleanups.parse("""
                month[normalize_month]
                pages[normalize_page_numbers]
                title[escapeAmpersands,escapeDollarSign,escapeUnderscores,latex_cleanup]
                booktitle[escapeAmpersands,escapeDollarSign,escapeUnderscores,latex_cleanup]
                publisher[escapeAmpersands,escapeDollarSign,escapeUnderscores,latex_cleanup]
                journal[escapeAmpersands,escapeDollarSign,escapeUnderscores,latex_cleanup]
                abstract[escapeAmpersands,escapeDollarSign,escapeUnderscores,latex_cleanup]
                """);

        List<FieldFormatterCleanup> expected = new ArrayList<>(30);
        expected.add(new FieldFormatterCleanup(StandardField.MONTH, new NormalizeMonthFormatter()));
        expected.add(new FieldFormatterCleanup(StandardField.PAGES, new NormalizePagesFormatter()));
        for (Field field : List.of(StandardField.TITLE, StandardField.BOOKTITLE, StandardField.PUBLISHER, StandardField.JOURNAL, StandardField.ABSTRACT)) {
            expected.add(new FieldFormatterCleanup(field, new EscapeAmpersandsFormatter()));
            expected.add(new FieldFormatterCleanup(field, new EscapeDollarSignFormatter()));
            expected.add(new FieldFormatterCleanup(field, new EscapeUnderscoresFormatter()));
            expected.add(new FieldFormatterCleanup(field, new LatexCleanupFormatter()));
        }

        assertEquals(expected, fieldFormatterCleanups);
    }

    @Test
    void parserParsesLatexCleanupFormatter() {
        List<FieldFormatterCleanup> fieldFormatterCleanups = Cleanups.parse("""
                title[latex_cleanup]
                """);
        assertEquals(
                List.of(new FieldFormatterCleanup(StandardField.TITLE, new LatexCleanupFormatter())),
                fieldFormatterCleanups);
    }

    @Test
    void parserParsesTwoFormatters() {
        List<FieldFormatterCleanup> fieldFormatterCleanups = Cleanups.parse("""
                title[escapeUnderscores,latex_cleanup]
                """);
        assertEquals(
                List.of(
                        new FieldFormatterCleanup(StandardField.TITLE, new EscapeUnderscoresFormatter()),
                        new FieldFormatterCleanup(StandardField.TITLE, new LatexCleanupFormatter())
                ),
                fieldFormatterCleanups);
    }

    @Test
    void parserParsesFourFormatters() {
        List<FieldFormatterCleanup> fieldFormatterCleanups = Cleanups.parse("""
                title[escapeAmpersands,escapeDollarSign,escapeUnderscores,latex_cleanup]
                """);
        assertEquals(
                List.of(
                        new FieldFormatterCleanup(StandardField.TITLE, new EscapeAmpersandsFormatter()),
                        new FieldFormatterCleanup(StandardField.TITLE, new EscapeDollarSignFormatter()),
                        new FieldFormatterCleanup(StandardField.TITLE, new EscapeUnderscoresFormatter()),
                        new FieldFormatterCleanup(StandardField.TITLE, new LatexCleanupFormatter())
                ),
                fieldFormatterCleanups);
    }

    @Test
    void parserParsesTwoFormattersWithCommas() {
        List<FieldFormatterCleanup> fieldFormatterCleanups = Cleanups.parse("""
                title[escapeUnderscores,latex_cleanup]
                booktitle[escapeAmpersands,escapeDollarSign]
                """);
        assertEquals(
                List.of(
                        new FieldFormatterCleanup(StandardField.TITLE, new EscapeUnderscoresFormatter()),
                        new FieldFormatterCleanup(StandardField.TITLE, new LatexCleanupFormatter()),
                        new FieldFormatterCleanup(StandardField.BOOKTITLE, new EscapeAmpersandsFormatter()),
                        new FieldFormatterCleanup(StandardField.BOOKTITLE, new EscapeDollarSignFormatter())
                ),
                fieldFormatterCleanups);
    }

    @Test
    void parserParsesTwoFormattersOneWithComma() {
        List<FieldFormatterCleanup> fieldFormatterCleanups = Cleanups.parse("""
                pages[normalize_page_numbers]
                booktitle[escapeAmpersands,escapeDollarSign]
                """);
        assertEquals(
                List.of(
                        new FieldFormatterCleanup(StandardField.PAGES, new NormalizePagesFormatter()),
                        new FieldFormatterCleanup(StandardField.BOOKTITLE, new EscapeAmpersandsFormatter()),
                        new FieldFormatterCleanup(StandardField.BOOKTITLE, new EscapeDollarSignFormatter())
                ),
                fieldFormatterCleanups);
    }

    @Test
    void parserParsesThreeFormattersTwoWithComma() {
        List<FieldFormatterCleanup> fieldFormatterCleanups = Cleanups.parse("""
                pages[normalize_page_numbers]
                title[escapeUnderscores,latex_cleanup]
                booktitle[escapeAmpersands,escapeDollarSign]
                """);
        assertEquals(
                List.of(
                        new FieldFormatterCleanup(StandardField.PAGES, new NormalizePagesFormatter()),
                        new FieldFormatterCleanup(StandardField.TITLE, new EscapeUnderscoresFormatter()),
                        new FieldFormatterCleanup(StandardField.TITLE, new LatexCleanupFormatter()),
                        new FieldFormatterCleanup(StandardField.BOOKTITLE, new EscapeAmpersandsFormatter()),
                        new FieldFormatterCleanup(StandardField.BOOKTITLE, new EscapeDollarSignFormatter())
                ),
                fieldFormatterCleanups);
    }

    @Test
    void parserWithTwoAndThree() {
        List<FieldFormatterCleanup> fieldFormatterCleanups = Cleanups.parse("""
                title[escapeAmpersands,escapeUnderscores,latex_cleanup]
                booktitle[escapeAmpersands,escapeUnderscores,latex_cleanup]
                """);

        List<FieldFormatterCleanup> expected = new ArrayList<>(30);
        for (Field field : List.of(StandardField.TITLE, StandardField.BOOKTITLE)) {
            expected.add(new FieldFormatterCleanup(field, new EscapeAmpersandsFormatter()));
            expected.add(new FieldFormatterCleanup(field, new EscapeUnderscoresFormatter()));
            expected.add(new FieldFormatterCleanup(field, new LatexCleanupFormatter()));
        }

        assertEquals(expected, fieldFormatterCleanups);
    }

    @Test
    void parserWithFourEntries() {
        List<FieldFormatterCleanup> fieldFormatterCleanups = Cleanups.parse("""
                title[escapeUnderscores,latex_cleanup]
                booktitle[escapeAmpersands,escapeUnderscores,latex_cleanup]
                """);
        assertEquals(
                List.of(
                        new FieldFormatterCleanup(StandardField.TITLE, new EscapeUnderscoresFormatter()),
                        new FieldFormatterCleanup(StandardField.TITLE, new LatexCleanupFormatter()),
                        new FieldFormatterCleanup(StandardField.BOOKTITLE, new EscapeAmpersandsFormatter()),
                        new FieldFormatterCleanup(StandardField.BOOKTITLE, new EscapeUnderscoresFormatter()),
                        new FieldFormatterCleanup(StandardField.BOOKTITLE, new LatexCleanupFormatter())
                ),
                fieldFormatterCleanups);
    }

    @Test
    void parserTest() {
        List<FieldFormatterCleanup> fieldFormatterCleanups = Cleanups.parse("""
                title[escapeAmpersands,escapeUnderscores,latex_cleanup]
                booktitle[escapeAmpersands,latex_cleanup]
                """);
        assertEquals(
                List.of(
                        new FieldFormatterCleanup(StandardField.TITLE, new EscapeAmpersandsFormatter()),
                        new FieldFormatterCleanup(StandardField.TITLE, new EscapeUnderscoresFormatter()),
                        new FieldFormatterCleanup(StandardField.TITLE, new LatexCleanupFormatter()),
                        new FieldFormatterCleanup(StandardField.BOOKTITLE, new EscapeAmpersandsFormatter()),
                        new FieldFormatterCleanup(StandardField.BOOKTITLE, new LatexCleanupFormatter())
                ),
                fieldFormatterCleanups);
    }
}
