package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfPageScannerTest {

    @Test
    void scansFivePercentFromEachEnd() throws IOException {
        try (PDDocument document = createDocument(100)) {
            List<String> pages = PdfPageScanner.scanPages(document);

            // 5% of 100 = 5 pages per end
            assertEquals(10, pages.size());

            for (int i = 0; i < 5; i++) {
                assertTrue(pages.get(i).contains("Page " + (i + 1)));
            }

            for (int i = 0; i < 5; i++) {
                assertTrue(pages.get(i + 5).contains("Page " + (i + 96)));
            }
        }
    }

    @Test
    void roundsFivePercentUp() throws IOException {
        try (PDDocument document = createDocument(150)) {
            List<String> pages = PdfPageScanner.scanPages(document);

            // 5% of 150 = 7.5, rounded up to 8 pages per end
            assertEquals(16, pages.size());

            for (int i = 0; i < 8; i++) {
                assertTrue(pages.get(i).contains("Page " + (i + 1)));
            }

            for (int i = 0; i < 8; i++) {
                assertTrue(pages.get(i + 8).contains("Page " + (i + 143)));
            }
        }
    }

    @Test
    void usesMinimumThreePagesPerEnd() throws IOException {
        try (PDDocument document = createDocument(10)) {
            List<String> pages = PdfPageScanner.scanPages(document);

            // 5% of 10 = 0.5, rounded up to 1,
            // but the minimum is 3 pages per end
            assertEquals(6, pages.size());

            assertTrue(pages.get(0).contains("Page 1"));
            assertTrue(pages.get(1).contains("Page 2"));
            assertTrue(pages.get(2).contains("Page 3"));

            assertTrue(pages.get(3).contains("Page 8"));
            assertTrue(pages.get(4).contains("Page 9"));
            assertTrue(pages.get(5).contains("Page 10"));
        }
    }

    @Test
    void usesMaximumTenPagesPerEnd() throws IOException {
        try (PDDocument document = createDocument(500)) {
            List<String> pages = PdfPageScanner.scanPages(document);

            // 5% of 500 = 25,
            // but the maximum is 10 pages per end
            assertEquals(20, pages.size());

            for (int i = 0; i < 10; i++) {
                assertTrue(pages.get(i).contains("Page " + (i + 1)));
            }

            for (int i = 0; i < 10; i++) {
                assertTrue(pages.get(i + 10).contains("Page " + (i + 491)));
            }
        }
    }

    @Test
    void doesNotDuplicatePagesInVeryShortDocument() throws IOException {
        try (PDDocument document = createDocument(4)) {
            List<String> pages = PdfPageScanner.scanPages(document);

            // Minimum would normally request 3 pages from each end,
            // but a 4-page document should return each page only once
            assertEquals(4, pages.size());

            for (int i = 0; i < 4; i++) {
                assertTrue(pages.get(i).contains("Page " + (i + 1)));
            }
        }
    }

    private PDDocument createDocument(int numberOfPages) throws IOException {
        PDDocument document = new PDDocument();

        for (int i = 1; i <= numberOfPages; i++) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(
                        new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                        12);
                contentStream.newLineAtOffset(50, 700);
                contentStream.showText("Page " + i);
                contentStream.endText();
            }
        }

        return document;
    }
}
