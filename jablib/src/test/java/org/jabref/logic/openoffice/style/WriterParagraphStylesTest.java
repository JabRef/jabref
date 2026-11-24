package org.jabref.logic.openoffice.style;

import java.util.List;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@NullMarked
class WriterParagraphStylesTest {

    @Test
    void bibliographyTitleFormatsPreserveIdentifiersAndOrder() {
        List<String> expected = List.of(
                "Bibliography Heading",
                "Title",
                "Text body",
                "Subtitle",
                "Heading 1",
                "Heading 2",
                "Heading 3",
                "Heading 4");

        List<String> actual = WriterParagraphStyles.BIBLIOGRAPHY_TITLE_FORMATS;

        assertEquals(expected, actual);
    }

    @Test
    void bibliographyBodyFormatsPreserveIdentifiersAndOrder() {
        List<String> expected = List.of(
                "Bibliography 1",
                "Text body",
                "Text body indent",
                "First line indent",
                "Hanging indent",
                "List",
                "List Indent",
                "Marginalia");

        List<String> actual = WriterParagraphStyles.BIBLIOGRAPHY_BODY_FORMATS;

        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @MethodSource("paragraphStyleCatalogs")
    void paragraphStyleCatalogRejectsAddition(List<String> catalog) {
        assertThrows(UnsupportedOperationException.class, () -> catalog.add("Custom style"));
    }

    @ParameterizedTest
    @MethodSource("paragraphStyleCatalogs")
    void paragraphStyleCatalogRejectsReplacement(List<String> catalog) {
        assertThrows(UnsupportedOperationException.class, () -> catalog.set(0, "Custom style"));
    }

    @ParameterizedTest
    @MethodSource("paragraphStyleCatalogs")
    void paragraphStyleCatalogRejectsRemoval(List<String> catalog) {
        assertThrows(UnsupportedOperationException.class, catalog::removeFirst);
    }

    private static Stream<List<String>> paragraphStyleCatalogs() {
        return Stream.of(WriterParagraphStyles.BIBLIOGRAPHY_TITLE_FORMATS, WriterParagraphStyles.BIBLIOGRAPHY_BODY_FORMATS);
    }

    @Test
    void defaultBibliographyBodyFormatPreservesIdentifier() {
        assertEquals("Text body", WriterParagraphStyles.DEFAULT_BIBLIOGRAPHY_BODY_FORMAT);
    }

    @Test
    void defaultHangingIndentBibliographyBodyFormatPreservesIdentifier() {
        assertEquals("Hanging indent", WriterParagraphStyles.DEFAULT_HANGING_INDENT_BIBLIOGRAPHY_BODY_FORMAT);
    }
}
