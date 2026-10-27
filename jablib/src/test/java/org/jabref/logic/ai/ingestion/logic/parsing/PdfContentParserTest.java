package org.jabref.logic.ai.ingestion.logic.parsing;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
class PdfContentParserTest {

    private final PdfContentParser parser = new PdfContentParser();

    @Test
    void parseNonExistentFileReturnsEmptyList() {
        List<String> pages = parser.parse(Path.of("non-existent-file.pdf"));
        assertEquals(List.of(), pages);
    }

    @Test
    void parsePdfFileReturnsContentPerPage(@TempDir Path tempDir) throws IOException {
        Path pdfPath = tempDir.resolve("test.pdf");

        try (PDDocument document = new PDDocument()) {
            PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            PDPage firstPage = new PDPage();
            document.addPage(firstPage);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, firstPage)) {
                contentStream.beginText();
                contentStream.setFont(font, 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("First page text");
                contentStream.endText();
            }

            PDPage secondPage = new PDPage();
            document.addPage(secondPage);
            try (PDPageContentStream contentStream = new PDPageContentStream(document, secondPage)) {
                contentStream.beginText();
                contentStream.setFont(font, 12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("Second page text");
                contentStream.endText();
            }

            document.save(pdfPath.toFile());
        }

        List<String> pages = parser.parse(pdfPath);
        assertEquals(2, pages.size());
        assertTrue(pages.getFirst().contains("First page text"));
        assertTrue(pages.get(1).contains("Second page text"));

        Optional<String> parsedAsString = parser.parseAsString(pdfPath);
        assertTrue(parsedAsString.isPresent());
        assertTrue(parsedAsString.get().contains("First page text"));
        assertTrue(parsedAsString.get().contains("Second page text"));
    }
}
