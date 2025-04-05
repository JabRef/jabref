package org.jabref.logic.ocr;

import java.nio.file.Path;
import java.util.Set;

import org.jabref.logic.ocr.exception.OcrProcessException;
import org.jabref.logic.ocr.models.OcrEngineConfig;
import org.jabref.logic.ocr.models.OcrLanguage;
import org.jabref.logic.ocr.models.OcrResult;

/**
 * Interface defining OCR (Optical Character Recognition) operations.
 * <p>
 * This interface serves as a port in JabRef's hexagonal architecture, allowing
 * different OCR engines to be plugged in via adapters.
 */
public interface OcrService {

    /**
     * Process a PDF file using OCR to extract text.
     *
     * @param pdfPath Path to the PDF file
     * @return OCR result containing extracted text and metadata
     * @throws OcrProcessException if OCR processing fails
     */
    OcrResult processPdf(Path pdfPath) throws OcrProcessException;
    
    /**
     * Process an image file using OCR to extract text.
     *
     * @param imagePath Path to the image file
     * @return OCR result containing extracted text and metadata
     * @throws OcrProcessException if OCR processing fails
     */
    OcrResult processImage(Path imagePath) throws OcrProcessException;
    
    /**
     * Add OCR-extracted text as a searchable layer to a PDF file.
     *
     * @param pdfPath Path to the source PDF file
     * @param outputPath Path to save the modified PDF
     * @param ocrResult OCR result containing extracted text to add
     * @throws OcrProcessException if adding the text layer fails
     */
    void addTextLayerToPdf(Path pdfPath, Path outputPath, OcrResult ocrResult) throws OcrProcessException;
    
    /**
     * Set the language for OCR processing.
     *
     * @param language OCR language to use
     * @throws OcrProcessException if the language is not supported or cannot be set
     */
    void setLanguage(OcrLanguage language) throws OcrProcessException;
    
    /**
     * Get supported languages for this OCR engine.
     *
     * @return Set of supported languages
     */
    Set<OcrLanguage> getSupportedLanguages();
    
    /**
     * Get the name of the OCR engine.
     *
     * @return Engine name
     */
    String getEngineName();
    
    /**
     * Check if the OCR engine is available and properly configured.
     *
     * @return true if the engine is ready to use
     */
    boolean isAvailable();
    
    /**
     * Apply a specific configuration to the OCR engine.
     *
     * @param config Configuration to apply
     * @throws OcrProcessException if the configuration cannot be applied
     */
    void applyConfig(OcrEngineConfig config) throws OcrProcessException;
}