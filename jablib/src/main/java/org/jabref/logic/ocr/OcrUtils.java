package org.jabref.logic.ocr;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.jabref.logic.util.HeadlessExecutorService;
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

    /// Upper bound on how much of a failing OCR process's output is kept for the failure
    /// dialog. A verbose engine should not be able to grow this without bound.
    private static final int MAX_CAPTURED_OUTPUT_LENGTH = 3000;

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
        String commandLine = command.stream()
                                    .map(OcrUtils::quoteIfNeeded)
                                    .collect(Collectors.joining(" "));
        StringBuilder outputBuilder = new StringBuilder();
        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();

            // Draining the output concurrently, rather than after process.waitFor(), avoids a
            // deadlock: the process can block trying to write to a full output pipe if nothing
            // reads it while the process is still running.
            StreamGobbler streamGobblerInput = new StreamGobbler(process.getInputStream(), line -> {
                LOGGER.debug(line);
                if (outputBuilder.length() < MAX_CAPTURED_OUTPUT_LENGTH) {
                    outputBuilder.append(line).append(System.lineSeparator());
                }
            });
            Future<?> gobblerFuture = HeadlessExecutorService.INSTANCE.execute(() -> {
                streamGobblerInput.run();
                return null;
            });

            // A single wait, bounded by the real timeout. Once the gobbler task finishes, the
            // process's output stream has closed, so the process has exited or is about to;
            // process.waitFor() below then returns immediately. This replaces waiting twice
            // (once on the process, once on the gobbler with its own separate timeout), which
            // is also what left a window for outputBuilder to be read while still being written.
            try {
                gobblerFuture.get(OcrUtils.TIMEOUT_MINS, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                process.destroyForcibly();
                return OcrResult.failure(OcrFailureReason.TIMEOUT, commandLine, outputBuilder.toString());
            } catch (ExecutionException e) {
                LOGGER.error("Error while reading output of {}.", engineName, e);
                return OcrResult.failure(OcrFailureReason.IO_ERROR, commandLine, outputBuilder.toString());
            }

            process.waitFor();

            if (process.exitValue() == 0) {
                return OcrResult.success(null); // The output file path will be determined by the specific OCR engine implementation
            } else {
                return OcrResult.failure(OcrFailureReason.NON_ZERO_EXIT, commandLine, outputBuilder.toString());
            }
        } catch (IOException e) {
            LOGGER.error("Error while running {}.", engineName, e);
            return OcrResult.failure(OcrFailureReason.IO_ERROR, commandLine, outputBuilder.toString());
        } catch (InterruptedException e) {
            if (process != null) {
                process.destroyForcibly();
            }
            Thread.currentThread().interrupt();
            LOGGER.error("{} process was interrupted.", engineName, e);
            return OcrResult.failure(OcrFailureReason.INTERRUPTED, commandLine, outputBuilder.toString());
        }
    }

    /// Wraps an argument in double quotes if it contains whitespace, so a command line built by
    /// joining arguments with spaces (for display only, not re-parsed) does not read as having
    /// more arguments than it does, e.g. a path like `/Program Files/engine`.
    private static String quoteIfNeeded(String argument) {
        if (argument.isEmpty() || argument.chars().anyMatch(Character::isWhitespace)) {
            return "\"" + argument + "\"";
        }
        return argument;
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
