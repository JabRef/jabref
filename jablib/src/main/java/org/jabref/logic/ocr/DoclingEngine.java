package org.jabref.logic.ocr;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.jabref.logic.util.strings.StringUtil;

/// Implementation of the {@link OcrEngine} interface using Docling.
public class DoclingEngine implements OcrEngine {

    private final OcrPreferences ocrPreferences;

    public DoclingEngine(OcrPreferences ocrPreferences) {
        this.ocrPreferences = ocrPreferences;
    }

    @Override
    public String getName() {
        return "Docling";
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
                OcrEngineUtils.LOGGER.debug("Checking Docling availability timed out");
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException e) {
            OcrEngineUtils.LOGGER.error("Docling is not available at {}: IOException occurred", ocrPreferences.getOcrEnginePath(), e);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            OcrEngineUtils.LOGGER.error("Checking Docling availability was interrupted", e);
            return false;
        }
    }

    @Override
    public OcrResult performOcrAndEmbedText(Path pdfPath) {
        if (!isAvailable()) {
            return OcrResult.failure(OcrFailureReason.NOT_AVAILABLE);
        }
        Path outputPath = OcrEngineUtils.makeOutputFilePath(pdfPath);
        String outputFile = outputPath.toString();
    }
}
