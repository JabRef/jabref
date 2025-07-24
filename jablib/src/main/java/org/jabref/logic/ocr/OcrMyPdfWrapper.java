package org.jabref.logic.ocr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates searchable PDFs using ocrmypdf command-line tool.
 */
public class OcrMyPdfWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(OcrMyPdfWrapper.class);
    private static final int TIMEOUT_MINUTES = 10;

    /**
     * Checks if ocrmypdf is available on the system.
     */
    public static boolean isAvailable() {
        try {
            Process process = new ProcessBuilder("ocrmypdf", "--version")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            LOGGER.debug("ocrmypdf not found on system", e);
            return false;
        }
    }

    /**
     * Creates a searchable PDF using ocrmypdf.
     *
     * @param inputPath Path to input PDF
     * @param outputPath Path for output searchable PDF
     * @param options Additional options for ocrmypdf
     * @return OcrResult with success/failure status
     */
    public static OcrResult createSearchablePdf(Path inputPath, Path outputPath, OcrMyPdfOptions options) {
        if (!isAvailable()) {
            return OcrResult.failure("ocrmypdf is not installed. Please install it using: pip install ocrmypdf");
        }

        List<String> command = new ArrayList<>();
        command.add("ocrmypdf");

        // Add options
        if (options.skipTextPages) {
            command.add("--skip-text");
        }
        if (options.clean) {
            command.add("--clean");
        }
        if (options.language != null && !options.language.isEmpty()) {
            command.add("-l");
            command.add(options.language);
        }
        if (options.forceOcr) {
            command.add("--force-ocr");
        }

        // Optimization options
        command.add("--optimize");
        command.add(String.valueOf(options.optimizeLevel));

        // Show progress
        command.add("--verbose");
        command.add("1");

        // Input and output files
        command.add(inputPath.toString());
        command.add(outputPath.toString());

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            StringBuilder extractedText = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    LOGGER.debug("ocrmypdf: {}", line);

                    // Try to capture some text from the output for preview
                    if (line.contains("Text:") || line.contains("OCR:")) {
                        extractedText.append(line).append("\n");
                    }
                }
            }

            boolean finished = process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                return OcrResult.failure("OCR process timed out after " + TIMEOUT_MINUTES + " minutes");
            }

            if (process.exitValue() == 0) {
                String resultText = extractedText.length() > 0 ?
                        extractedText.toString() :
                        "Searchable PDF created successfully with ocrmypdf";
                return OcrResult.success(resultText, outputPath);
            } else {
                return OcrResult.failure("ocrmypdf failed with exit code " + process.exitValue() + ": " + output.toString());
            }

        } catch (IOException | InterruptedException e) {
            LOGGER.error("Error running ocrmypdf", e);
            return OcrResult.failure("Error running ocrmypdf: " + e.getMessage());
        }
    }

    /**
     * Options for ocrmypdf.
     */
    public static class OcrMyPdfOptions {
        public boolean skipTextPages = true;
        public boolean clean = false;
        public boolean forceOcr = false;
        public String language = "eng";
        public int optimizeLevel = 1;

        public static OcrMyPdfOptions defaults() {
            return new OcrMyPdfOptions();
        }

        public static OcrMyPdfOptions forceOcr() {
            OcrMyPdfOptions options = new OcrMyPdfOptions();
            options.forceOcr = true;
            options.skipTextPages = false;
            return options;
        }
    }
}
