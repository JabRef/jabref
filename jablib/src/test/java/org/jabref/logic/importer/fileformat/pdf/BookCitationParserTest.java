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
}
