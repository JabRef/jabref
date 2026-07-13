package org.jabref.logic.ocr;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.logic.util.StreamGobbler;
import org.jabref.logic.util.strings.StringUtil;

/// Implementation of the {@link OcrEngine} interface using OCRmyPDF.
public class OcrMyPdfEngine implements OcrEngine {

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
        ArrayList<String> command = StringUtil.splitRespectingEscapedWhitespace(ocrPreferences.getOcrEnginePath());
        command.add("--version");
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            boolean finished = process.waitFor(OcrEngineUtils.CHECKING_TIMEOUT, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                OcrEngineUtils.LOGGER.debug("Checking OCRmyPDF availability timed out");
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException e) {
            OcrEngineUtils.LOGGER.error("OCRmyPDF is not available at {}: IOException occurred", ocrPreferences.getOcrEnginePath(), e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            OcrEngineUtils.LOGGER.error("Checking OCRmyPDF availability was interrupted", e);
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
        Path outputPath = OcrEngineUtils.makeOutputFilePath(pdfPath);
        String outputFile = outputPath.toString();
        String ocrCommand = switch (ocrPreferences.getPagesHaveText()) {
            case SKIP ->
                    "--skip-text";
            case FORCE ->
                    "--force-ocr";
            case REDO ->
                    "--redo-ocr";
        };
        // although a list of Strings, it represents a single command as that is how the ProcessBuilder expects it.
        ArrayList<String> command = StringUtil.splitRespectingEscapedWhitespace(ocrPreferences.getOcrEnginePath());
        command.add(ocrCommand);
        command.add(pdfPath.toString());
        command.add(outputFile);
        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();

            // Get the output and the errors of the process
            StreamGobbler streamGobblerInput = new StreamGobbler(process.getInputStream(), OcrEngineUtils.LOGGER::debug);
            HeadlessExecutorService.INSTANCE.execute(streamGobblerInput);

            boolean finished = process.waitFor(OcrEngineUtils.TIMEOUT_MINS, TimeUnit.MINUTES);
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
            OcrEngineUtils.LOGGER.error("Error while running OCRmyPDF.", e);
            return OcrResult.failure(OcrFailureReason.IO_ERROR);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            OcrEngineUtils.LOGGER.error("OCRmyPDF process was interrupted.", e);
            return OcrResult.failure(OcrFailureReason.INTERRUPTED);
        }
    }
}
