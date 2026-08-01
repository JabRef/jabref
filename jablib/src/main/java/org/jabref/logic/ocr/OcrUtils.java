package org.jabref.logic.ocr;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.util.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OcrUtils {

    public static final String OCR_PDF_PREFIX = "_ocr.pdf";
    public static final int TIMEOUT_MINS = 10;
    public static final int CHECKING_TIMEOUT = 5;
    public static final Logger LOGGER = LoggerFactory.getLogger(OcrUtils.class);

    /// Checks if the OCR engine is available for use.
    ///
    /// @return true if the engine is available, false otherwise.
    public static boolean isAvailable(OcrPreferences ocrPreferences) {
        ArrayList<String> command = StringUtil.splitRespectingEscapedWhitespace(ocrPreferences.getOcrEnginePath());
        command.add("--version");
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            boolean finished = process.waitFor(OcrUtils.CHECKING_TIMEOUT, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                LOGGER.debug("Checking OCR engine availability timed out");
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException e) {
            LOGGER.error("OCR engine is not available at {}: IOException occurred", ocrPreferences.getOcrEnginePath(), e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Checking OCR engine availability was interrupted", e);
            return false;
        }
    }

    /// Generates the output path for the searchable PDF.
    ///
    /// Example: Documents/my files/document.pdf -> Documents/my files/document_ocr.pdf.
    ///
    /// @param inputPath the path of the PDF that needs to be OCRed.
    /// @return the output path of the searchable OCRed PDF.
    public static Path makeOutputFilePath(Path inputPath) {
        String baseName = FileUtil.getBaseName(inputPath.toString());
        return inputPath.resolveSibling(baseName + OCR_PDF_PREFIX);
    }
}
