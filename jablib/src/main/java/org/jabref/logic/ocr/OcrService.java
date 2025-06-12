package org.jabref.logic.ocr;

import java.io.File;
import java.nio.file.Path;

import org.jabref.model.strings.StringUtil;

import com.sun.jna.Platform;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for performing Optical Character Recognition (OCR) on PDF files.
 * This class provides a high-level interface to OCR functionality,
 * abstracting away the specific OCR engine implementation details.
 */
public class OcrService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OcrService.class);
    private static final String JNA_LIBRARY_PATH = "jna.library.path";
    // The OCR engine instance
    private final Tesseract tesseract;

    /**
     * Constructs a new OcrService with default settings.
     * Currently uses Tesseract with English language support.
     */
    public OcrService() {
        if (Platform.isMac()) {
            if (Platform.isARM()) {
                System.setProperty(JNA_LIBRARY_PATH, JNA_LIBRARY_PATH + File.pathSeparator + "/opt/homebrew/lib/");
            } else {
                System.setProperty(JNA_LIBRARY_PATH, JNA_LIBRARY_PATH + File.pathSeparator + "/usr/local/cellar/");
            }
        }
        this.tesseract = new Tesseract();

        // Configure Tesseract
        tesseract.setLanguage("eng");

        // TODO: This path needs to be configurable and bundled properly
        // For now, we'll use a relative path that works during development
        tesseract.setDatapath("tessdata");

        LOGGER.debug("Initialized OcrService with Tesseract");
    }

    /**
     * Performs OCR on a PDF file and returns the extracted text.
     *
     * @param pdfPath Path to the PDF file to process
     * @return The extracted text, or empty string if no text found
     * @throws OcrException if OCR processing fails
     */
    public String performOcr(Path pdfPath) throws OcrException {
        // Validate input
        if (pdfPath == null) {
            throw new OcrException("PDF path cannot be null");
        }

        File pdfFile = pdfPath.toFile();
        if (!pdfFile.exists()) {
            throw new OcrException("PDF file does not exist: " + pdfPath);
        }

        try {
            LOGGER.info("Starting OCR for file: {}", pdfFile.getName());

            // Perform OCR
            String result = tesseract.doOCR(pdfFile);

            // Clean up the result (remove extra whitespace, etc.)
            result = StringUtil.isBlank(result) ? "" : result.trim();

            LOGGER.info("OCR completed successfully. Extracted {} characters", result.length());
            return result;
        } catch (
                TesseractException e) {
            LOGGER.error("OCR failed for file: {}", pdfFile.getName(), e);
            throw new OcrException(
                    "Failed to perform OCR on file: " + pdfFile.getName() +
                            ". Error: " + e.getMessage(), e
            );
        }
    }
}
