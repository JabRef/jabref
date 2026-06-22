package org.jabref.logic.ocr;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.logic.util.StreamGobbler;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.util.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Implementation of the {@link OcrEngine} interface using OCRmyPDF.
public class OcrMyPdfEngine implements OcrEngine {

    private static final String OCR_PDF_PREFIX = "_ocr.pdf";

    private static final Logger LOGGER = LoggerFactory.getLogger(OcrMyPdfEngine.class);
    private static final int TIMEOUT_MINS = 10;
    private static final int CHECKING_TIMEOUT = 5;

    private final OcrPreferences ocrPreferences;

    public OcrMyPdfEngine(OcrPreferences ocrPreferences) {
        this.ocrPreferences = ocrPreferences;
    }

    @Override
    public String getName() {
        return "OCRmyPDF";
    }

    @Override
    public boolean isAvailable() {
        ArrayList<String> command = StringUtil.splitPathRespectingQuotes(ocrPreferences.getOcrPath());
        command.add("--version");
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            boolean finished = process.waitFor(CHECKING_TIMEOUT, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                LOGGER.debug("Checking OCRmyPDF availability timed out");
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException e) {
            LOGGER.error("OCRmyPDF is not available: IOException occurred", e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Checking OCRmyPDF availability was interrupted", e);
            return false;
        }
    }

    /// OCRmyPDF writes the searchable PDF to a new file alongside the original file.
    ///
    /// Example: document.pdf -> document_ocr.pdf.
    ///
    /// @param pdfPath the file to perform OCR on.
    /// @return {@link OcrResult.Success} containing the path to the searchable PDF,
    /// or {@link OcrResult.Failure} with an error message if OCR failed.
    @Override
    public OcrResult performOcrAndEmbedText(Path pdfPath) {
        if (!isAvailable()) {
            return OcrResult.failure(OcrFailureReason.NOT_AVAILABLE);
        }
        Path outputPath = makeOutputFilePath(pdfPath);
        String outputFile = outputPath.toString();
        // although a list of Strings, it represents a single command as that is how the ProcessBuilder expects it.
        ArrayList<String> command = StringUtil.splitPathRespectingQuotes(ocrPreferences.getOcrPath());
        command.add("--skip-text");
        command.add(pdfPath.toString());
        command.add(outputFile);
        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();

            // Get the output and the errors of the process
            StreamGobbler streamGobblerInput = new StreamGobbler(process.getInputStream(), LOGGER::debug);
            HeadlessExecutorService.INSTANCE.execute(streamGobblerInput);

            boolean finished = process.waitFor(TIMEOUT_MINS, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                return OcrResult.failure(OcrFailureReason.TIMEOUT);
            }

            if (process.exitValue() == 0) {
                return OcrResult.success(outputPath);
            } else {
                return OcrResult.failure(OcrFailureReason.NON_ZERO_EXIT);
            }
        } catch (IOException e) {
            LOGGER.error("Error while running OCRmyPDF.", e);
            return OcrResult.failure(OcrFailureReason.IO_ERROR);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            LOGGER.error("OCRmyPDF process was interrupted.", e);
            return OcrResult.failure(OcrFailureReason.INTERRUPTED);
        }
    }

    /// Generates the output path for the searchable PDF.
    ///
    /// Example: Documents/my files/document.pdf -> Documents/my files/document_ocr.pdf.
    ///
    /// @param inputPath the path of the PDF that needs to be OCRed.
    /// @return the output path of the searchable OCRed PDF.
    private Path makeOutputFilePath(Path inputPath) {
        String baseName = FileUtil.getBaseName(inputPath.toString());
        Path outputPath = inputPath.resolveSibling(baseName + OCR_PDF_PREFIX);
        return outputPath;
    }
}
