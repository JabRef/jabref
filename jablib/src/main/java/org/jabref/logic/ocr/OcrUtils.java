package org.jabref.logic.ocr;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.jabref.logic.util.StreamGobbler;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.util.strings.StringUtil;

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

    /// Helper method to abstract the common logic of running an OCR engine command and handling its output.
    public static OcrResult performOcr(ArrayList<String> command, String engineName) {
        String commandLine = String.join(" ", command);
        StringBuilder outputBuilder = new StringBuilder();
        Process process = null;
        Thread gobblerThread = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();

            // Get the output and the errors of the process
            StreamGobbler streamGobblerInput = new StreamGobbler(process.getInputStream(), line -> {
                LOGGER.debug(line);
                outputBuilder.append(line).append(System.lineSeparator());
            });
            gobblerThread = new Thread(streamGobblerInput, engineName + "-output-gobbler");
            gobblerThread.start();

            boolean finished = process.waitFor(OcrUtils.TIMEOUT_MINS, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                // destroyForcibly() closes the process's output pipe, so the gobbler hits EOF and
                // exits on its own shortly after; joining before reading outputBuilder avoids a
                // data race between this thread reading it and the gobbler still appending to it.
                joinGobblerQuietly(gobblerThread);
                return OcrResult.failure(OcrFailureReason.TIMEOUT, commandLine, outputBuilder.toString());
            }

            // The process has exited, so its output is fully written; wait for the gobbler to finish
            // draining it so the captured output is complete (and safe to read from this thread)
            // before it is read below.
            joinGobblerQuietly(gobblerThread);

            if (process.exitValue() == 0) {
                return OcrResult.success(null); // The output file path will be determined by the specific OCR engine implementation
            } else {
                return OcrResult.failure(OcrFailureReason.NON_ZERO_EXIT, commandLine, outputBuilder.toString());
            }
        } catch (IOException e) {
            LOGGER.error("Error while running {}.", engineName, e);
            return OcrResult.failure(OcrFailureReason.IO_ERROR, commandLine, outputBuilder.toString());
        } catch (InterruptedException e) {
            process.destroyForcibly();
            // See the TIMEOUT branch above: must join before reading outputBuilder here too.
            joinGobblerQuietly(gobblerThread);
            Thread.currentThread().interrupt();
            LOGGER.error("{} process was interrupted.", engineName, e);
            return OcrResult.failure(OcrFailureReason.INTERRUPTED, commandLine, outputBuilder.toString());
        }
    }

    /// Waits briefly for the output-gobbler thread to finish, so its buffered output is safe to
    /// read afterward. A null thread (the process never started) is a no-op.
    private static void joinGobblerQuietly(Thread gobblerThread) {
        if (gobblerThread == null) {
            return;
        }
        try {
            gobblerThread.join(TimeUnit.SECONDS.toMillis(5));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
