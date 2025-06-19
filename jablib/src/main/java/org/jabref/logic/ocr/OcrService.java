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
    private static final String TESSDATA_PREFIX = "TESSDATA_PREFIX";

    // The OCR engine instance
    private final Tesseract tesseract;

    /**
     * Constructs a new OcrService with default settings.
     * Currently uses Tesseract with English language support.
     */
    public OcrService() throws OcrException {
        configureLibraryPath();

        try {
            this.tesseract = new Tesseract();
            tesseract.setLanguage("eng");
            configureTessdata();
            LOGGER.debug("Initialized OcrService with Tesseract");
        } catch (Exception e) {
            throw new OcrException("Failed to initialize OCR engine", e);
        }
    }

    private void configureLibraryPath() {
        if (Platform.isMac()) {
            String originalPath = System.getProperty(JNA_LIBRARY_PATH, "");
            if (Platform.isARM()) {
                System.setProperty(JNA_LIBRARY_PATH,
                        originalPath + File.pathSeparator + "/opt/homebrew/lib/");
            } else {
                System.setProperty(JNA_LIBRARY_PATH,
                        originalPath + File.pathSeparator + "/usr/local/cellar/");
            }
        }
    }

    private void configureTessdata() throws OcrException {
        // First, check environment variable
        String tessdataPath = System.getenv(TESSDATA_PREFIX);

        if (tessdataPath != null && !tessdataPath.isEmpty()) {
            File tessdataDir = new File(tessdataPath);
            if (tessdataDir.exists() && tessdataDir.isDirectory()) {
                // Tesseract expects the parent directory of tessdata
                if (tessdataDir.getName().equals("tessdata")) {
                    tesseract.setDatapath(tessdataDir.getParent());
                } else {
                    tesseract.setDatapath(tessdataPath);
                }
                LOGGER.info("Using tessdata from environment variable: {}", tessdataPath);
                return;
            } else {
                LOGGER.warn("TESSDATA_PREFIX points to non-existent directory: {}", tessdataPath);
            }
        }

        // Fall back to system locations
        String systemPath = findSystemTessdata();
        if (systemPath != null) {
            tesseract.setDatapath(systemPath);
            LOGGER.info("Using system tessdata at: {}", systemPath);
        } else {
            throw new OcrException("Could not find tessdata directory. Please set TESSDATA_PREFIX environment variable.");
        }
    }

    private String findSystemTessdata() {
        String[] possiblePaths = {
                "/usr/local/share",  // Homebrew Intel
                "/opt/homebrew/share",  // Homebrew ARM
                "/usr/share"  // System
        };

        for (String path : possiblePaths) {
            File tessdata = new File(path, "tessdata");
            File engData = new File(tessdata, "eng.traineddata");
            if (tessdata.exists() && engData.exists()) {
                return path;  // Return parent of tessdata
            }
        }

        return null;
    }

    /**
     * Performs OCR on a PDF file and returns the extracted text.
     *
     * @param pdfPath Path to the PDF file to process
     * @return The extracted text, or empty string if no text found
     * @throws OcrException if OCR processing fails
     */
    public OcrResult performOcr(Path pdfPath) {
        // User error - not an exception
        if (pdfPath == null) {
            LOGGER.warn("PDF path is null");
            return OcrResult.failure("No file path provided");
        }

        File pdfFile = pdfPath.toFile();

        // User error - not an exception
        if (!pdfFile.exists()) {
            LOGGER.warn("PDF file does not exist: {}", pdfPath);
            return OcrResult.failure("File does not exist: " + pdfPath.getFileName());
        }

        try {
            LOGGER.info("Starting OCR for file: {}", pdfFile.getName());

            String result = tesseract.doOCR(pdfFile);
            result = StringUtil.isBlank(result) ? "" : result.trim();

            LOGGER.info("OCR completed successfully. Extracted {} characters", result.length());
            return OcrResult.success(result);

        } catch (TesseractException e) {
            // This could be either a user error (corrupt PDF) or our bug
            // Log it as error but return as failure, not exception
            LOGGER.error("OCR processing failed for file: {}", pdfFile.getName(), e);
            return OcrResult.failure("Failed to extract text from PDF: " + e.getMessage());
        }
    }
}
