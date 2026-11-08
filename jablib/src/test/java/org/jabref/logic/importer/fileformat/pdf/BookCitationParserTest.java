package org.jabref.logic.importer.fileformat.pdf;

import java.util.function.UnaryOperator;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookCitationParserTest {

    @Test
    void parsesCompleteCitation() {
        String citation = """
                Коваль О. П.
                К56 Збірник задач з фізики для 8 класу / О. П. Коваль, Н. І. Ткач. — 2-ге вид., перероб. — Харків : Ранок, 2010. — 176 с.
                """;

        BibEntry result = BookCitationParser.parse(citation, UnaryOperator.identity());

        assertEquals(new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Збірник задач з фізики для 8 класу")
                .withField(StandardField.AUTHOR, "О. П. Коваль, Н. І. Ткач")
                .withField(StandardField.PUBLISHER, "Ранок")
                .withField(StandardField.YEAR, "2010")
                .withField(StandardField.EDITION, "2"), result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n", "\r", "\u0085", "\u2028", "\u2029"})
    void parsesWrappedCitationThroughLocator(String lineBreak) {
        String citation = "Коваль О. П." + lineBreak + "К56 Збірник задач /" + lineBreak
                + "О. П. Коваль." + lineBreak + "— 2-ге вид., перероб. —" + lineBreak
                + "Харків : Ранок, 2010.";

        assertEquals(new BibEntry(StandardEntryType.Book)
                        .withField(StandardField.TITLE, "Збірник задач")
                        .withField(StandardField.AUTHOR, "О. П. Коваль")
                        .withField(StandardField.PUBLISHER, "Ранок")
                        .withField(StandardField.YEAR, "2010")
                        .withField(StandardField.EDITION, "2"),
                BookCitationParser.parse(BookCitationBlock.find(citation).orElseThrow(), UnaryOperator.identity()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n", "\r", "\u0085", "\u2028", "\u2029"})
    void parsesWrappedEditorCitationDirectly(String lineBreak) {
        String citation = "HTML5 /" + lineBreak + "за ред. О. П. Коваля." + lineBreak
                + "— 2-ге вид., перероб. —" + lineBreak + "Харків : Ранок, 2010.";

        assertEquals(new BibEntry(StandardEntryType.Book)
                        .withField(StandardField.TITLE, "HTML5")
                        .withField(StandardField.EDITOR, "О. П. Коваля")
                        .withField(StandardField.PUBLISHER, "Ранок")
                        .withField(StandardField.YEAR, "2010")
                        .withField(StandardField.EDITION, "2"),
                BookCitationParser.parse(citation, UnaryOperator.identity()));
    }

    @Test
    void parsesStandaloneSlashWithAuthorHeading() {
        String citation = """
                Редько О. П.
                PHP7 для початківців
                /
                Редько О. П. — Харків : Ранок, 2010.
                """;

        assertEquals(new BibEntry(StandardEntryType.Book)
                        .withField(StandardField.TITLE, "PHP7 для початківців")
                        .withField(StandardField.AUTHOR, "Редько О. П")
                        .withField(StandardField.PUBLISHER, "Ранок")
                        .withField(StandardField.YEAR, "2010"),
                BookCitationParser.parse(BookCitationBlock.find(citation).orElseThrow(), UnaryOperator.identity()));
    }

    @Test
    void preservesMissingOptionalFields() {
        assertEquals(new BibEntry(StandardEntryType.Book)
                        .withField(StandardField.TITLE, "Збірник задач")
                        .withField(StandardField.AUTHOR, "О. П. Коваль"),
                BookCitationParser.parse("Збірник задач / О. П. Коваль", UnaryOperator.identity()));
    }

    // [utest->req~import.pdf.book-citation-publication~1]
    @Test
    void retainsOtherFieldsWhenPublisherIsAmbiguous() {
        String citation = """
                Коваль О. П.
                К56 Збірник задач / О. П. Коваль, Н. І. Ткач. — 2-ге вид., перероб. — Київ : Генеза ; Харків : Ранок, 2010. — 176 с.
                """;
        BibEntry expected = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.TITLE, "Збірник задач")
                .withField(StandardField.AUTHOR, "О. П. Коваль, Н. І. Ткач")
                .withField(StandardField.YEAR, "2010")
                .withField(StandardField.EDITION, "2");

        assertEquals(expected, BookCitationParser.parse(citation, UnaryOperator.identity()));
    }

    // [utest->req~import.pdf.book-citation-publication~1]
    @Test
    void retainsPublicationFieldsWithoutTitleContributorsOrEdition() {
        String citation = "Збірник задач з фізики : навч. посіб. — Харків : Ранок, 2010. — 176 с.";
        BibEntry expected = new BibEntry(StandardEntryType.Book)
                .withField(StandardField.PUBLISHER, "Ранок")
                .withField(StandardField.YEAR, "2010");

        assertEquals(expected, BookCitationParser.parse(citation, UnaryOperator.identity()));
    }

    @Test
    void leavesAmbiguousPublicationFieldsEmpty() {
        String citation = "Збірник / О. П. Коваль. — Харків : Ранок, 2010. — Київ : Генеза, 2011.";

        assertEquals(new BibEntry(StandardEntryType.Book)
                        .withField(StandardField.TITLE, "Збірник")
                        .withField(StandardField.AUTHOR, "О. П. Коваль"),
                BookCitationParser.parse(citation, UnaryOperator.identity()));
    }

    @Test
    void returnsEmptyFieldsForUnrelatedText() {
        assertEquals(new BibEntry(StandardEntryType.Book),
                BookCitationParser.parse("Chapter 1: HTML5/PHP7", UnaryOperator.identity()));
    }

    // [utest->req~import.pdf.book-citation-publication~1]
    @ParameterizedTest
    @ValueSource(strings = {"Харків : Ранок, 2010–2012.", "Харків : Ранок\nРекомендовано до друку, 2015."})
    void retainsContributorsWithoutInventingPublicationFields(String publication) {
        String citation = "Збірник задач / О. П. Коваль. — " + publication;

        assertEquals(new BibEntry(StandardEntryType.Book)
                        .withField(StandardField.TITLE, "Збірник задач")
                        .withField(StandardField.AUTHOR, "О. П. Коваль"),
                BookCitationParser.parse(citation, UnaryOperator.identity()));
    }
}
