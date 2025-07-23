package org.jabref.logic.ocr;

import java.nio.file.Path;

import org.jabref.logic.FilePreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for performing OCR (Optical Character Recognition) on PDF files.
 * This service uses an OcrProvider implementation to perform the actual OCR.
 */
public class OcrService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OcrService.class);

    private final OcrProvider ocrProvider;

    /**
     * Constructs a new OcrService with the default provider (Tesseract).
     * Uses system defaults for configuration.
     */
    public OcrService() {
        this(null);
    }

    /**
     * Constructs a new OcrService with the default provider (Tesseract).
     *
     * @param filePreferences The file preferences containing OCR settings (can be null)
     */
    public OcrService(FilePreferences filePreferences) {
        // In the future, we could check preferences to determine which provider to use
        // For now, always use Tesseract
        this.ocrProvider = new TesseractOcrProvider(filePreferences);

        if (ocrProvider.isAvailable()) {
            LOGGER.info("Initialized OcrService with provider: {}", ocrProvider.getName());
        } else {
            LOGGER.warn("OCR provider '{}' is not available: {}",
                    ocrProvider.getName(),
                    ocrProvider.getConfigurationError());
        }
    }

    /**
     * Performs OCR on a PDF file and returns the extracted text.
     *
     * @param pdfPath Path to the PDF file to process
     * @return The extracted text result
     */
    public OcrResult performOcr(Path pdfPath) {
        if (!ocrProvider.isAvailable()) {
            return OcrResult.failure("OCR provider '" + ocrProvider.getName() +
                    "' is not available: " + ocrProvider.getConfigurationError());
        }
        return ocrProvider.performOcr(pdfPath);
    }

    /**
     * Gets the name of the current OCR provider.
     */
    public String getProviderName() {
        return ocrProvider.getName();
    }

    /**
     * Checks if the OCR service is available.
     */
    public boolean isAvailable() {
        return ocrProvider.isAvailable();
    }
}
