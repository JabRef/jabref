package org.jabref.logic.ocr;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.rendering.PDFRenderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates searchable PDFs by overlaying invisible text on existing PDFs.
 */
public class SearchablePdfCreator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchablePdfCreator.class);
    private static final PDFont FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final float FONT_SIZE = 12f;

    /**
     * Creates a searchable PDF by overlaying the OCR text invisibly on the original PDF.
     *
     * @param inputPdfPath Path to the original PDF
     * @param outputPdfPath Path where the searchable PDF will be saved
     * @param ocrText The OCR-extracted text
     * @return true if successful, false otherwise
     */
    public boolean createSearchablePdf(Path inputPdfPath, Path outputPdfPath, String ocrText) {
        try (PDDocument document = Loader.loadPDF(inputPdfPath.toFile())) {
            // Split text by pages
            List<String> pageTexts = distributeTextAcrossPages(ocrText, document.getNumberOfPages());

            for (int i = 0; i < document.getNumberOfPages(); i++) {
                PDPage page = document.getPage(i);
                String pageText = pageTexts.get(i);

                if (!pageText.isEmpty()) {
                    addInvisibleText(document, page, pageText);
                }
            }

            document.save(outputPdfPath.toFile());
            LOGGER.info("Created searchable PDF: {}", outputPdfPath);
            return true;

        } catch (IOException e) {
            LOGGER.error("Failed to create searchable PDF", e);
            return false;
        }
    }

    /**
     * Distributes text across pages. This is a simple implementation that splits text evenly.
     * A more sophisticated implementation would use page-level OCR data.
     */
    private List<String> distributeTextAcrossPages(String fullText, int pageCount) {
        if (pageCount == 1) {
            return List.of(fullText);
        }

        String[] words = fullText.split("\\s+");
        int wordsPerPage = Math.max(1, words.length / pageCount);

        List<String> pageTexts = new java.util.ArrayList<>();
        int startIndex = 0;

        for (int i = 0; i < pageCount; i++) {
            int endIndex = (i == pageCount - 1) ? words.length : Math.min(startIndex + wordsPerPage, words.length);

            if (startIndex < words.length) {
                String pageText = String.join(" ", java.util.Arrays.copyOfRange(words, startIndex, endIndex));
                pageTexts.add(pageText);
                startIndex = endIndex;
            } else {
                pageTexts.add("");
            }
        }

        return pageTexts;
    }

    /**
     * Adds invisible text to a PDF page.
     */
    private void addInvisibleText(PDDocument document, PDPage page, String text) throws IOException {
        PDRectangle pageSize = page.getMediaBox();

        try (PDPageContentStream contentStream = new PDPageContentStream(
                document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

            // Make text invisible
            contentStream.setRenderingMode(RenderingMode.fromInt(3)); // Neither fill nor stroke (invisible)
            contentStream.setFont(FONT, FONT_SIZE);

            // Start text at bottom of page (won't be visible anyway)
            contentStream.beginText();
            contentStream.newLineAtOffset(0, 0);

            // Write text in chunks to avoid issues with long lines
            String[] words = text.split("\\s+");
            StringBuilder line = new StringBuilder();

            for (String word : words) {
                if (line.length() + word.length() > 100) {
                    contentStream.showText(line.toString());
                    contentStream.newLine();
                    line = new StringBuilder();
                }
                line.append(word).append(" ");
            }

            if (line.length() > 0) {
                contentStream.showText(line.toString());
            }

            contentStream.endText();
        }
    }
}
