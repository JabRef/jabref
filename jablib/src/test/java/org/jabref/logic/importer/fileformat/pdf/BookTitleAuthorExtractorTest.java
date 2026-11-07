package org.jabref.logic.importer.fileformat.pdf;

import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class BookTitleAuthorExtractorTest {

    private static final String CITATION = """
            Коваль О. П.
            К56 Збірник задач з фізики для 8 класу / О. П. Коваль, Н. І. Ткач. — 2-ге вид., перероб. — Харків : Ранок, 2010. — 176 с.
            """;

    @ParameterizedTest
    @ValueSource(strings = {"С40", "С 40", "С\u00A040"})
    void removesCyrillicCatalogueCode(String code) {
        assertEquals(Optional.of("Фізика"),
                BookTitleAuthorExtractor.findTitle(code + " Фізика / В. Д. Сиротюк. - Харків : Сиция, 2011."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Windows 11", "ISO 9001", "C 40", "С 40-річчям", "40 задач", "Фізика С 40"})
    void preservesLegitimateTitleText(String title) {
        assertEquals(Optional.of(title),
                BookTitleAuthorExtractor.findTitle(title + " / В. Д. Сиротюк. - Харків : Сиция, 2011."));
    }

    @ParameterizedTest
    @ValueSource(strings = {"стан\u00ADдарту", "стан\u00AD дарту", "стан\u00AD\nдарту", "стан\u00AD\r\nдарту"})
    void joinsWordsBrokenBySoftHyphen(String word) {
        assertEquals(Optional.of("Фізика: (рівень стандарту)"),
                BookTitleAuthorExtractor.findTitle("Фізика: (рівень " + word + ") / В. Д. Сиротюк. - Харків : Сиция, 2011."));
    }

    @Test
    void preservesOrdinaryHyphensAndWordBoundaries() {
        assertEquals(Optional.of("Фізико-математичні задачі для 11-го класу"),
                BookTitleAuthorExtractor.findTitle("Фізико-математичні задачі\nдля 11-го класу / В. Д. Сиротюк. - Харків : Сиция, 2011."));
    }

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

    @ParameterizedTest
    @ValueSource(strings = {"\n", "\r\n", "\r", "\u0085", "\u2028", "\u2029"})
    void handlesLineBreaks(String lineBreak) {
        String citation = CITATION.replace("\n", lineBreak);

        assertEquals(
                Optional.of("Збірник задач з фізики для 8 класу"),
                BookTitleAuthorExtractor.findTitle(citation));
    }
}
