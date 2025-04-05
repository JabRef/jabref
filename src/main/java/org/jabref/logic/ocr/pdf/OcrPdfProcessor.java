package org.jabref.logic.ocr.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.jabref.logic.ocr.OcrService;
import org.jabref.logic.ocr.exception.OcrProcessException;
import org.jabref.logic.ocr.models.OcrResult;
import org.jabref.logic.search.IndexManager;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processor for PDF OCR operations.
 * <p>
 * This class coordinates the process of extracting text from PDFs using OCR,
 * adding text layers, and indexing the results. It demonstrates how OCR would
 * integrate with JabRef's PDF handling and search indexing.
 */
public class OcrPdfProcessor {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(OcrPdfProcessor.class);
    
    private final OcrService ocrService;
    private final IndexManager indexManager;
    
    /**
     * Create a new PDF OCR processor.
     *
     * @param ocrService OCR service to use
     * @param indexManager Index manager for search indexing
     */
    public OcrPdfProcessor(OcrService ocrService, IndexManager indexManager) {
        this.ocrService = ocrService;
        this.indexManager = indexManager;
    }
    
    /**
     * Process a PDF file with OCR and add a searchable text layer.
     *
     * @param pdfPath Path to the PDF file
     * @param outputPath Path to save the processed PDF
     * @return OCR result
     * @throws OcrProcessException if processing fails
     */
    public OcrResult processAndAddTextLayer(Path pdfPath, Path outputPath) throws OcrProcessException {
        // Check if PDF already has text
        if (hasTextLayer(pdfPath)) {
            LOGGER.info("PDF already has a text layer, no OCR needed: {}", pdfPath);
            return new OcrResult.Builder()
                    .withExtractedText(extractTextFromPdf(pdfPath))
                    .withEngineName("Existing PDF Text")
                    .withSourceFile(pdfPath)
                    .build();
        }
        
        // Perform OCR
        OcrResult result = ocrService.processPdf(pdfPath);
        
        // Add text layer to PDF
        TextLayerAdder.addTextLayer(pdfPath, outputPath, result);
        
        return result;
    }
    
    /**
     * Process a PDF and index the extracted text.
     *
     * @param pdfPath Path to the PDF file
     * @param documentId Document ID for indexing
     * @return OCR result
     * @throws OcrProcessException if processing fails
     */
    public OcrResult processAndIndex(Path pdfPath, String documentId) throws OcrProcessException {
        // Extract text from PDF (either existing or using OCR)
        OcrResult result = hasTextLayer(pdfPath) 
                ? new OcrResult.Builder()
                    .withExtractedText(extractTextFromPdf(pdfPath))
                    .withEngineName("Existing PDF Text")
                    .withSourceFile(pdfPath)
                    .build()
                : ocrService.processPdf(pdfPath);
        
        // Index the extracted text
        indexManager.addDocumentToIndex(documentId, result.getExtractedText());
        
        return result;
    }
    
    /**
     * Extract text from a PDF if it already has a text layer.
     *
     * @param pdfPath Path to the PDF file
     * @return Extracted text
     * @throws OcrProcessException if text extraction fails
     */
    private String extractTextFromPdf(Path pdfPath) throws OcrProcessException {
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            throw new OcrProcessException("Failed to extract text from PDF: " + e.getMessage(), e);
        }
    }
    
    /**
     * Check if a PDF already has a text layer.
     *
     * @param pdfPath Path to the PDF file
     * @return true if the PDF has a text layer
     */
    private boolean hasTextLayer(Path pdfPath) {
        return TextLayerAdder.hasTextLayer(pdfPath);
    }
    
    /**
     * Create a searchable text layer from OCR result.
     *
     * @param ocrResult OCR result
     * @param pageCount Number of pages in the PDF
     * @return Searchable text layer
     */
    public SearchableTextLayer createSearchableTextLayer(OcrResult ocrResult, int pageCount) {
        Map<Integer, String> pageTextMap = new HashMap<>();
        
        // In a real implementation, we would have more sophisticated text distribution
        String[] paragraphs = ocrResult.getExtractedText().split("\n\n");
        int paragraphsPerPage = Math.max(1, (int) Math.ceil((double) paragraphs.length / pageCount));
        
        StringBuilder currentPage = new StringBuilder();
        int pageNumber = 1;
        int paragraphCount = 0;
        
        for (String paragraph : paragraphs) {
            currentPage.append(paragraph).append("\n\n");
            paragraphCount++;
            
            if (paragraphCount >= paragraphsPerPage && pageNumber < pageCount) {
                pageTextMap.put(pageNumber, currentPage.toString());
                currentPage = new StringBuilder();
                pageNumber++;
                paragraphCount = 0;
            }
        }
        
        // Add any remaining text
        if (currentPage.length() > 0) {
            pageTextMap.put(pageNumber, currentPage.toString());
        }
        
        return new SearchableTextLayer.Builder()
                .withSourceText(ocrResult.getExtractedText())
                .withPageTextMap(pageTextMap)
                .withInvisible(true)
                .build();
    }
}