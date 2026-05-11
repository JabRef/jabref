package org.jabref.logic.ocr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jabref.logic.util.io.FileUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the OcrEngine interface using OCRmyPDF.
 */
public class OcrMyPdfEngine implements OcrEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(OcrMyPdfEngine.class);
    private static final int TIMEOUT_MINS = 10;

    @Override
    public String getName() {
        return "OCRmyPDF";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     * OCRmyPDF writes the searchable PDF to a new file alongside the original file.
     * <p>
     * Example: document.pdf -> document_ocr.pdf.
     *
     * @param pdfPath the file to perform OCR on.
     * @return {@link OcrResult.Success} containing the path to the searchable PDF,
     * or {@link OcrResult.Failure} with an error message if OCR failed.
     */
    @Override
    public OcrResult performOcr(Path pdfPath) {
        if (!isAvailable()) {
            return OcrResult.failure("OCRmyPDF is not installed. Please install it using: pip install OCRmyPDF");
        }
        List<String> command = new ArrayList<>();
        command.add("ocrmypdf");
        command.add(pdfPath.toString());
        Path outputPath = makeOutputFilePath(pdfPath);
        String outputFile = outputPath.toString();
        command.add(outputFile);
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            StringBuilder processOutput = new StringBuilder();

            // Get the output and the errors of the process
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    processOutput.append(line).append("\n");
                    LOGGER.debug("OCRmyPDF: {}", line);
                }
            }

            boolean finished = process.waitFor(TIMEOUT_MINS, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                return OcrResult.failure("OCRmyPDF process timed out after " + TIMEOUT_MINS + " minutes.");
            }

            if (process.exitValue() == 0) {
                String result = "A searchable PDF has been created with OCRmyPDF at: " + outputFile;
                return OcrResult.success(result, outputPath);
            } else {
                return OcrResult.failure("OCRmyPDF failed with exit code " + process.exitValue() + ": " + processOutput.toString());
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.error("Error while running OCRmyPDF.", e);
            return OcrResult.failure("Error while running OCRmyPDF: " + e.getMessage());
        }
    }

    /**
     * Generates the output path for the searchable PDF.
     * Example: Documents/my files/document.pdf -> Documents/my files/document_ocr.pdf.
     *
     * @param inputPath the path of the PDF that needs to be OCRed.
     * @return the output path of the searchable OCRed PDF.
     */
    private Path makeOutputFilePath(Path inputPath) {
        String baseName = FileUtil.getBaseName(inputPath.toString());
        Path outputPath = inputPath.resolveSibling(baseName + "_ocr.pdf");
        return outputPath;
    }
}
