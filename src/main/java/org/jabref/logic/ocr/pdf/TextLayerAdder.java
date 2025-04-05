package org.jabref.logic.ocr.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.ocr.exception.OcrProcessException;
import org.jabref.logic.ocr.models.OcrResult;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.encoding.WinAnsiEncoding;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for adding searchable text layers to PDFs.
 * <p>
 * This class demonstrates how to use PDFBox to add OCR-extracted text
 * as a searchable layer to PDF documents.
 */
public class TextLayerAdder {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TextLayerAdder.class);
    
    private TextLayerAdder() {
        // Utility class, no public constructor
    }
    
    /**
     * Add a searchable text layer to a PDF.
     *
     * @param sourcePdf Path to source PDF
     * @param outputPdf Path to save the output PDF
     * @param ocrResult OCR result containing text to add
     * @throws OcrProcessException if adding the text layer fails
     */
    public static void addTextLayer(Path sourcePdf, Path outputPdf, OcrResult ocrResult) throws OcrProcessException {
        try (PDDocument document = PDDocument.load(sourcePdf.toFile())) {
            
            // Split text into pages (in a real implementation, we would match text to page layout)
            List<String> pageTexts = splitTextIntoPages(ocrResult.getExtractedText(), document.getNumberOfPages());
            
            // Process each page
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                if (i < pageTexts.size()) {
                    addTextToPage(document, document.getPage(i), pageTexts.get(i));
                }
            }
            
            // Save the document with the text layer
            document.save(outputPdf.toFile());
            LOGGER.info("Added searchable text layer to PDF: {}", outputPdf);
            
        } catch (IOException e) {
            throw new OcrProcessException("Failed to add text layer to PDF: " + e.getMessage(), e);
        }
    }
    
    /**
     * Add text to a PDF page as a searchable layer.
     * <p>
     * In a real implementation, this would use PDFBox's more advanced features
     * to properly position text according to the page layout.
     *
     * @param document PDF document
     * @param page PDF page
     * @param text Text to add
     * @throws IOException if adding text fails
     */
    private static void addTextToPage(PDDocument document, PDPage page, String text) throws IOException {
        PDRectangle pageRect = page.getMediaBox();
        float fontSize = 1; // Very small to make it invisible
        
        // Use a content stream to add text (in a real implementation, we would position text properly)
        try (PDPageContentStream contentStream = new PDPageContentStream(
                document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            
            PDFont font = PDType1Font.HELVETICA;
            contentStream.setFont(font, fontSize);
            contentStream.beginText();
            
            // Position text (in a real implementation, this would be more sophisticated)
            contentStream.newLineAtOffset(0, 0);
            
            // Set text rendering mode to invisible (3 = invisible)
            contentStream.setRenderingMode(3);
            
            // Add text, handling character encoding
            addEncodedText(contentStream, text, font);
            
            contentStream.endText();
        }
    }
    
    /**
     * Add text handling character encoding issues.
     * <p>
     * This method breaks text into smaller chunks and handles characters
     * that may not be supported by the font.
     *
     * @param contentStream PDF content stream
     * @param text Text to add
     * @param font PDF font
     * @throws IOException if adding text fails
     */
    private static void addEncodedText(PDPageContentStream contentStream, String text, PDFont font) throws IOException {
        // In a real implementation, we would handle character encoding more robustly
        // For now, we'll just filter out characters that aren't in WinAnsiEncoding
        
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (WinAnsiEncoding.INSTANCE.contains(c)) {
                builder.append(c);
            } else if (c == '\n') {
                // Output current line and start a new one
                contentStream.showText(builder.toString());
                builder.setLength(0);
                contentStream.newLine();
            }
        }
        
        // Output any remaining text
        if (builder.length() > 0) {
            contentStream.showText(builder.toString());
        }
    }
    
    /**
     * Split text into pages.
     * <p>
     * In a real implementation, this would use more sophisticated logic
     * to match text to page layouts.
     *
     * @param text Full text
     * @param numPages Number of pages
     * @return List of text for each page
     */
    private static List<String> splitTextIntoPages(String text, int numPages) {
        List<String> pages = new ArrayList<>();
        
        // Simple approach: split by paragraphs and distribute
        String[] paragraphs = text.split("\n\n");
        int paragraphsPerPage = Math.max(1, (int) Math.ceil((double) paragraphs.length / numPages));
        
        StringBuilder currentPage = new StringBuilder();
        int paragraphCount = 0;
        
        for (String paragraph : paragraphs) {
            currentPage.append(paragraph).append("\n\n");
            paragraphCount++;
            
            if (paragraphCount >= paragraphsPerPage) {
                pages.add(currentPage.toString());
                currentPage = new StringBuilder();
                paragraphCount = 0;
            }
        }
        
        // Add any remaining text
        if (currentPage.length() > 0) {
            pages.add(currentPage.toString());
        }
        
        // If we didn't generate enough pages, add empty ones
        while (pages.size() < numPages) {
            pages.add("");
        }
        
        return pages;
    }
    
    /**
     * Check if a PDF already has a text layer.
     *
     * @param pdfPath Path to PDF
     * @return true if PDF has a text layer
     */
    public static boolean hasTextLayer(Path pdfPath) {
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            
            // If we get more than a threshold of characters, assume there's a text layer
            return text.trim().length() > 50;
            
        } catch (IOException e) {
            LOGGER.error("Failed to check if PDF has text layer: {}", e.getMessage());
            return false;
        }
    }
}