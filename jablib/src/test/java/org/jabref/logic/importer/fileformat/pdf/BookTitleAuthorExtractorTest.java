
package org.jabref.logic.importer.fileformat.pdf;

import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class BookTitleAuthorExtractorTest {

    private static final String CITATION = """
            Коваль О. П.
            К56 Збірник задач з фізики для 8 класу / О. П. Коваль, Н. І. Ткач. — 2-ге вид., перероб. — Харків : Ранок, 2010. — 176 с.
            """;

    @Test
    void extractsBookTitle() {
        assertEquals(
                Optional.of("Збірник задач з фізики для 8 класу"),
                BookTitleAuthorExtractor.findTitle(CITATION));
    }

    @Test
    void extractsMultipleAuthors() {
        assertEquals(
                Optional.of("О. П. Коваль, Н. І. Ткач"),
                BookTitleAuthorExtractor.findAuthors(CITATION));
    }

    @Test
    void extractsSingleAuthor() {
        String citation = "К56 Збірник задач / О. П. Коваль. — Харків : Ранок, 2010.";

        assertEquals(
                Optional.of("О. П. Коваль"),
                BookTitleAuthorExtractor.findAuthors(citation));
    }

    @Test
    void extractsWrappedTitle() {
        String citation = """
                Коваль О. П.
                К56 Збірник задач з фізики для 8 класу загальноосвітніх
                навчальних закладів / О. П. Коваль. — Харків : Ранок, 2010.
                """;

        assertEquals(
                Optional.of("Збірник задач з фізики для 8 класу загальноосвітніх навчальних закладів"),
                BookTitleAuthorExtractor.findTitle(citation));
    }

    @Test
    void extractsTitleWithoutAuthorHeading() {
        String citation = "К56 Збірник задач / О. П. Коваль. — Харків : Ранок, 2010.";

        assertEquals(
                Optional.of("Збірник задач"),
                BookTitleAuthorExtractor.findTitle(citation));
    }

    @Test
    void excludesPublicationMetadataFromAuthors() {
        assertEquals(
                Optional.of("О. П. Коваль, Н. І. Ткач"),
                BookTitleAuthorExtractor.findAuthors(CITATION));
    }

    @Test
    void returnsEmptyForMissingSeparator() {
        String citation = "Збірник задач з фізики — Харків : Ранок, 2010.";

        assertEquals(
                Optional.empty(),
                BookTitleAuthorExtractor.findTitle(citation));

        assertEquals(
                Optional.empty(),
                BookTitleAuthorExtractor.findAuthors(citation));
    }

    @Test
    void excludesExplicitlyLabelledEditorsFromAuthors() {
        String citation = "Збірник задач / за ред. О. П. Коваля. — Харків : Ранок, 2010.";

        assertEquals(
                Optional.empty(),
                BookTitleAuthorExtractor.findAuthors(citation));
    }

    @Test
    void extractsExplicitlyLabelledEditors() {
        String citation = "Збірник задач / за ред. О. П. Коваля. — Харків : Ранок, 2010.";

        assertEquals(
                Optional.of("О. П. Коваля"),
                BookTitleAuthorExtractor.findEditors(citation));
    }

    @Test
    void returnsEmptyEditorsWhenNotLabelled() {
        assertEquals(
                Optional.empty(),
                BookTitleAuthorExtractor.findEditors(CITATION));
    }

    @Test
    void preservesUkrainianCharacters() {
        String citation = "К56 Українська мова та література / Н. І. Ткач. — Київ : Генеза, 2015.";

        assertEquals(
                Optional.of("Українська мова та література"),
                BookTitleAuthorExtractor.findTitle(citation));

        assertEquals(
                Optional.of("Н. І. Ткач"),
                BookTitleAuthorExtractor.findAuthors(citation));
    }

    @Test
    void handlesWindowsLineBreaks() {
        String citation = CITATION.replace("\n", "\r\n");

        assertEquals(
                Optional.of("Збірник задач з фізики для 8 класу"),
                BookTitleAuthorExtractor.findTitle(citation));
    }
}
