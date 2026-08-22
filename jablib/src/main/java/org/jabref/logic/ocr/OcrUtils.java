package org.jabref.logic.ocr;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.logging.Level;

import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.util.strings.StringUtil;

import org.itsallcode.process.SimpleProcess;
import org.itsallcode.process.SimpleProcessBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OcrUtils {

    public static final String OCR_PDF_PREFIX = "_ocr.pdf";
    public static final int TIMEOUT_MINS = 10;
    public static final int CHECKING_TIMEOUT = 10;
    public static final Logger LOGGER = LoggerFactory.getLogger(OcrUtils.class);

    /// Checks if the OCR engine is available for use.
    ///
    /// @return true if the engine is available, false otherwise.
    private static boolean isAvailable(ArrayList<String> enginePath) {
        enginePath.add("--version");
        SimpleProcess<String> process;
        try {
            process = SimpleProcessBuilder.create()
                                          .command(enginePath)
                                          .redirectErrorStream(true)
                                          .streamLogLevel(Level.FINE)
                                          .start();
        } catch (UncheckedIOException e) {
            LOGGER.debug("OCR engine executable not found: {}", enginePath, e);
            return false;
        }

        try {
            process.waitForTermination(Duration.ofSeconds(OcrUtils.CHECKING_TIMEOUT));
            process.destroyForcibly();
            return process.exitValue() == 0;
        } catch (IllegalStateException e) {
            process.destroyForcibly();
            LOGGER.debug("Checking OCR engine availability timed out");
            return false;
        }
    }

    /// Helper method to abstract the common logic of running an OCR engine command and handling its output.
    public static OcrResult performOcr(OcrPreferences ocrPreferences, ArrayList<String> command) {
        ArrayList<String> enginePath = StringUtil.splitRespectingEscapedWhitespace(ocrPreferences.getOcrEnginePath());
        if (!isAvailable(enginePath)) {
            return OcrResult.failure(OcrFailureReason.NOT_AVAILABLE);
        }

        SimpleProcess<String> process;
        try {
            process = SimpleProcessBuilder.create()
                                          .command(command)
                                          .redirectErrorStream(true)
                                          .streamLogLevel(Level.FINE)
                                          .start();
        } catch (UncheckedIOException e) {
            LOGGER.error("Failed to start OCR process: {}", command, e);
            return OcrResult.failure(OcrFailureReason.IO_ERROR);
        }

        try {
            process.waitForTermination(Duration.ofMinutes(OcrUtils.TIMEOUT_MINS));
        } catch (IllegalStateException e) {
            process.destroyForcibly();
            LOGGER.debug("Performing OCR timed out");
            return OcrResult.failure(OcrFailureReason.TIMEOUT);
        }

        if (process.exitValue() == 0) {
            return OcrResult.success(null); // The output file path will be determined by the specific OCR engine implementation
        } else {
            return OcrResult.failure(OcrFailureReason.NON_ZERO_EXIT);
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
