package org.jabref.logic.importer.fileformat.pdf;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookCitationParserTest {

    @Test
    void parsesCompleteCitation() {
        String citation = """
                Коваль О. П.
                К56 Збірник задач з фізики для 8 класу / О. П. Коваль, Н. І. Ткач. — 2-ге вид., перероб. — Харків : Ранок, 2010. — 176 с.
                """;

        ParsedCitation result = BookCitationParser.parse(citation);

        assertEquals(
                Optional.of("Збірник задач з фізики для 8 класу"),
                result.title());

        assertEquals(
                Optional.of("О. П. Коваль, Н. І. Ткач"),
                result.authors());

        assertEquals(Optional.empty(), result.editors());
        assertEquals(Optional.of("Ранок"), result.publisher());
        assertEquals(Optional.of("2010"), result.year());
        assertEquals(Optional.of("2"), result.edition());
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
}
