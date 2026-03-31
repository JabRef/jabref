package org.jabref.logic.search.indexing;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.model.search.LinkedFilesConstants;

import org.apache.lucene.document.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DocumentReaderTest {

    private static Stream<Arguments> getLinesToMerge() {
        return Stream.of(
                Arguments.of("Sentences end with periods.", "Sentences end\nwith periods."),
                Arguments.of("Text is usually wrapped with hyphens.", "Text is us-\nually wrapp-\ned with hyphens."),
                Arguments.of("Longer texts often have both.", "Longer te-\nxts often\nhave both."),
                Arguments.of("No lines to break here", "No lines to break here")
        );
    }

    @ParameterizedTest
    @MethodSource("getLinesToMerge")
    public void mergeLinesTest(String expected, String linesToMerge) {
        String result = DocumentReader.mergeLines(linesToMerge);
        assertEquals(expected, result);
    }

    @Test
    void scannedPdfHasNoExtractableContent() {
        DocumentReader reader = new DocumentReader();
        List<Document> pages = reader.readPdfContents(
                "scanned-image-only.pdf",
                Path.of("src/test/resources/pdfs/scanned-image-only.pdf")
        );

        assertFalse(pages.isEmpty());

        for (Document page : pages) {
            assertNull(page.getField(LinkedFilesConstants.CONTENT.toString()));
        }
    }
}
