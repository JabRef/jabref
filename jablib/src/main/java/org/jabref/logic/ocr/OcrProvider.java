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
}
