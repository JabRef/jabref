package org.jabref.logic.importer.fileformat.pdf;

import java.util.Optional;

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

        ParsedCitation result = BookCitationParser.parse(citation);

        assertEquals(new ParsedCitation(
                Optional.of("Збірник задач з фізики для 8 класу"),
                Optional.of("О. П. Коваль, Н. І. Ткач"), Optional.empty(),
                Optional.of("Ранок"), Optional.of("2010"), Optional.of("2")), result);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n", "\r", "\u0085", "\u2028", "\u2029"})
    void parsesWrappedCitationThroughLocator(String lineBreak) {
        String citation = "Коваль О. П." + lineBreak + "К56 Збірник задач /" + lineBreak
                + "О. П. Коваль." + lineBreak + "— 2-ге вид., перероб. —" + lineBreak
                + "Харків : Ранок, 2010.";

        assertEquals(new ParsedCitation(
                        Optional.of("Збірник задач"), Optional.of("О. П. Коваль"), Optional.empty(),
                        Optional.of("Ранок"), Optional.of("2010"), Optional.of("2")),
                BookCitationParser.parse(BookCitationBlock.find(citation).orElseThrow()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n", "\r", "\u0085", "\u2028", "\u2029"})
    void parsesWrappedEditorCitationDirectly(String lineBreak) {
        String citation = "HTML5 /" + lineBreak + "за ред. О. П. Коваля." + lineBreak
                + "— 2-ге вид., перероб. —" + lineBreak + "Харків : Ранок, 2010.";

        assertEquals(new ParsedCitation(
                        Optional.of("HTML5"), Optional.empty(), Optional.of("О. П. Коваля"),
                        Optional.of("Ранок"), Optional.of("2010"), Optional.of("2")),
                BookCitationParser.parse(citation));
    }

    @Test
    void parsesStandaloneSlashWithAuthorHeading() {
        String citation = """
                Редько О. П.
                PHP7 для початківців
                /
                Редько О. П. — Харків : Ранок, 2010.
                """;

        assertEquals(new ParsedCitation(
                        Optional.of("PHP7 для початківців"), Optional.of("Редько О. П"), Optional.empty(),
                        Optional.of("Ранок"), Optional.of("2010"), Optional.empty()),
                BookCitationParser.parse(BookCitationBlock.find(citation).orElseThrow()));
    }

    @Test
    void preservesMissingOptionalFields() {
        assertEquals(new ParsedCitation(
                        Optional.of("Збірник задач"), Optional.of("О. П. Коваль"), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty()),
                BookCitationParser.parse("Збірник задач / О. П. Коваль"));
    }

    // [utest->req~import.pdf.book-citation-publication~1]
    @Test
    void retainsOtherFieldsWhenPublisherIsAmbiguous() {
        String citation = """
                Коваль О. П.
                К56 Збірник задач / О. П. Коваль, Н. І. Ткач. — 2-ге вид., перероб. — Київ : Генеза ; Харків : Ранок, 2010. — 176 с.
                """;
        ParsedCitation expected = new ParsedCitation(
                Optional.of("Збірник задач"),
                Optional.of("О. П. Коваль, Н. І. Ткач"),
                Optional.empty(),
                Optional.empty(),
                Optional.of("2010"),
                Optional.of("2"));

        assertEquals(expected, BookCitationParser.parse(citation));
    }

    // [utest->req~import.pdf.book-citation-publication~1]
    @Test
    void retainsPublicationFieldsWithoutTitleContributorsOrEdition() {
        String citation = "Збірник задач з фізики : навч. посіб. — Харків : Ранок, 2010. — 176 с.";
        ParsedCitation expected = new ParsedCitation(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("Ранок"),
                Optional.of("2010"),
                Optional.empty());

        assertEquals(expected, BookCitationParser.parse(citation));
    }

    @Test
    void leavesAmbiguousPublicationFieldsEmpty() {
        String citation = "Збірник / О. П. Коваль. — Харків : Ранок, 2010. — Київ : Генеза, 2011.";

        assertEquals(new ParsedCitation(
                        Optional.of("Збірник"), Optional.of("О. П. Коваль"), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty()),
                BookCitationParser.parse(citation));
    }

    @Test
    void returnsEmptyFieldsForUnrelatedText() {
        assertEquals(new ParsedCitation(
                        Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty()),
                BookCitationParser.parse("Chapter 1: HTML5/PHP7"));
    }
}
