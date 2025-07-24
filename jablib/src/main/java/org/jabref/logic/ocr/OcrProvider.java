package org.jabref.logic.ocr;

import java.nio.file.Path;

/**
 * Interface for OCR providers.
 * This abstraction allows JabRef to support multiple OCR engines in the future.
 */
public interface OcrProvider {

    /**
     * Performs OCR on the given PDF file.
     *
     * @param pdfPath The path to the PDF file
     * @return The OCR result containing extracted text or error message
     */
    OcrResult performOcr(Path pdfPath);

    /**
     * Checks if this OCR provider is available and properly configured.
     *
     * @return true if the provider is ready to use
     */
    boolean isAvailable();

    /**
     * Gets the display name of this OCR provider.
     *
     * @return The provider name (e.g., "Tesseract", "Google Cloud Vision")
     */
    String getName();

    /**
     * Gets a description of configuration issues if the provider is not available.
     *
     * @return Configuration error message, or empty string if properly configured
     */
    String getConfigurationError();

    /**
     * Creates a searchable PDF by performing OCR and embedding the text.
     *
     * @param inputPdfPath Path to the input PDF
     * @param outputPdfPath Path where the searchable PDF will be saved
     * @param method The method to use for creating searchable PDF
     * @return OcrResult containing the extracted text and output file path on success
     */
    OcrResult createSearchablePdf(Path inputPdfPath, Path outputPdfPath, OcrMethod method);

    /**
     * Checks if a specific OCR method is available.
     *
     * @param method The OCR method to check
     * @return true if the method is available
     */
    boolean isMethodAvailable(OcrMethod method);
}
