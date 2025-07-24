package org.jabref.logic.ocr;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jabref.model.strings.StringUtil;
import org.jabref.logic.FilePreferences;

import com.sun.jna.Platform;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tesseract-based implementation of the OCR provider.
 */
public class TesseractOcrProvider implements OcrProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(TesseractOcrProvider.class);
    private static final String JNA_LIBRARY_PATH = "jna.library.path";
    private static final String TESSDATA_PREFIX = "TESSDATA_PREFIX";
    private static final String TESSDATA_FOLDER_NAME = "tessdata";

    // Default tessdata paths for different operating systems
    private static final String DEFAULT_WIN_TESSDATA_PATH = "C:\\Program Files\\Tesseract-OCR\\tessdata";
    private static final String DEFAULT_WIN_TESSDATA_PATH_X86 = "C:\\Program Files (x86)\\Tesseract-OCR\\tessdata";
    private static final String DEFAULT_OSX_TESSDATA_PATH = "/usr/local/share/tessdata";
    private static final String DEFAULT_OSX_HOMEBREW_ARM_PATH = "/opt/homebrew/share/tessdata";
    private static final String DEFAULT_LINUX_TESSDATA_PATH = "/usr/share/tesseract-ocr/4.00/tessdata";
    private static final String DEFAULT_LINUX_TESSDATA_PATH_ALT = "/usr/share/tessdata";

    private final Tesseract tesseract = new Tesseract();
    private final FilePreferences filePreferences;

    private boolean isAvailable;
    private Exception configurationError = null;

    public TesseractOcrProvider(FilePreferences filePreferences) {
        this.filePreferences = filePreferences;
        this.isAvailable = false;

        try {
            configureLibraryPath();
            tesseract.setLanguage("eng");
            configureTessdata();
            this.isAvailable = true;
            LOGGER.debug("Initialized TesseractOcrProvider successfully");
        } catch (Exception e) {
            this.configurationError = e;
            LOGGER.error("Failed to initialize TesseractOcrProvider", e);
        }
    }

    @Override
    public OcrResult performOcr(Path pdfPath) {
        if (!isAvailable) {
            String errorDetails = configurationError != null ? configurationError.getMessage() : "Unknown configuration error";
            return OcrResult.failure("Tesseract OCR is not available: " + configurationError);
        }

        if (pdfPath == null) {
            LOGGER.warn("PDF path is null");
            return OcrResult.failure("No file path provided");
        }

        if (!Files.exists(pdfPath)) {
            LOGGER.warn("PDF file does not exist: {}", pdfPath);
            return OcrResult.failure("File does not exist: " + pdfPath.getFileName());
        }

        try {
            LOGGER.info("Starting Tesseract OCR for file: {}", pdfPath.getFileName());

            String result = tesseract.doOCR(pdfPath.toFile());
            result = StringUtil.isBlank(result) ? "" : result.trim();

            LOGGER.info("OCR completed successfully. Extracted {} characters", result.length());
            return OcrResult.success(result);

        } catch (TesseractException e) {
            LOGGER.error("OCR processing failed for file: {}", pdfPath.getFileName(), e);

            // Provide more helpful error messages
            if (e.getMessage().contains("tessdata") || e.getMessage().contains("traineddata")) {
                return OcrResult.failure("OCR failed: Language data not found. Please configure tessdata path in preferences or set TESSDATA_PREFIX environment variable.");
            }

            return OcrResult.failure("Failed to extract text from PDF: " + e.getMessage());
        }
    }

    @Override
    public boolean isAvailable() {
        return isAvailable;
    }

    @Override
    public String getName() {
        return "Tesseract";
    }

    @Override
    public String getConfigurationError() {
        if (configurationError == null) {
            return "";
        }
        return configurationError.getMessage() != null ? configurationError.getMessage() : "Unknown error";
    }

    /**
     * Creates a searchable PDF by performing OCR and overlaying the text invisibly.
     *
     * @param inputPdfPath Path to the input PDF
     * @param outputPdfPath Path where the searchable PDF will be saved
     * @return OcrResult indicating success with the output path or failure
     */
    public OcrResult createSearchablePdf(Path inputPdfPath, Path outputPdfPath) {
        // First, perform regular OCR to get the text
        OcrResult textResult = performOcr(inputPdfPath);

        if (textResult.isFailure()) {
            return textResult;
        }

        String extractedText = ((OcrResult.Success) textResult).text();

        // Create searchable PDF using the extracted text
        SearchablePdfCreator pdfCreator = new SearchablePdfCreator();
        boolean success = pdfCreator.createSearchablePdf(inputPdfPath, outputPdfPath, extractedText);

        if (success) {
            return OcrResult.success(extractedText, outputPdfPath);
        } else {
            return OcrResult.failure("Failed to create searchable PDF");
        }
    }

    private void configureLibraryPath() {
        if (Platform.isMac()) {
            String originalPath = System.getProperty(JNA_LIBRARY_PATH, "");
            if (Platform.isARM()) {
                System.setProperty(JNA_LIBRARY_PATH,
                        originalPath + java.io.File.pathSeparator + "/opt/homebrew/lib/");
            } else {
                System.setProperty(JNA_LIBRARY_PATH,
                        originalPath + java.io.File.pathSeparator + "/usr/local/cellar/");
            }
        }
    }

    private void configureTessdata() {
        // Priority 1: Check user preferences (from settings)
        if (filePreferences != null) {
            String prefPath = filePreferences.getOcrTessdataPath();
            if (!StringUtil.isBlank(prefPath) && setTessdataPath(prefPath)) {
                LOGGER.info("Using tessdata from preferences: {}", prefPath);
                return;
            }
        }

        // Priority 2: Check environment variable
        String envPath = System.getenv(TESSDATA_PREFIX);
        if (!StringUtil.isBlank(envPath) && setTessdataPath(envPath)) {
            LOGGER.info("Using tessdata from environment variable: {}", envPath);
            return;
        }

        // Priority 3: Try OS-specific default paths
        List<String> defaultPaths = getDefaultTessdataPaths();
        for (String defaultPath : defaultPaths) {
            if (setTessdataPath(defaultPath)) {
                LOGGER.info("Using default tessdata at: {}", defaultPath);
                return;
            }
        }

        // Could not find tessdata
        LOGGER.warn("Could not find tessdata directory. OCR will fail when attempted.");
        this.configurationError = new IllegalStateException("Could not find tessdata directory. Please configure in preferences or install Tesseract.");
    }

    /**
     * Attempts to set the tessdata path. Handles both cases where the path
     * points to tessdata directory or its parent.
     *
     * @param pathStr The path to check and set
     * @return true if the path was valid and set successfully
     */
    private boolean setTessdataPath(String pathStr) {
        try {
            Path path = Path.of(pathStr).toRealPath();
            LOGGER.debug("Original path: {}, Real path: {}", pathStr, path);

            // Case1: caller already gave the tessdata folder
            if (TESSDATA_FOLDER_NAME.equals(path.getFileName().toString())) {
                Path engData = path.resolve("eng.traineddata");
                LOGGER.debug("Looking for eng.traineddata at {}", engData);
                if (Files.isRegularFile(engData)) {
                    tesseract.setDatapath(path.toString());
                    return true;
                }
            }

            // Case2: caller gave parent directory
            Path tessdata = path.resolve(TESSDATA_FOLDER_NAME);
            Path engData   = tessdata.resolve("eng.traineddata");
            LOGGER.debug("Looking for tessdata at {}, eng.traineddata at {}", tessdata, engData);
            if (Files.isDirectory(tessdata) && Files.isRegularFile(engData)) {
                tesseract.setDatapath(tessdata.toString());
                return true;
            }

        } catch (Exception e) {
            LOGGER.debug("Invalid tessdata path: {}", pathStr, e);
        }
        return false;   // nothing usable found
    }


    /**
     * Gets the list of default tessdata paths based on the operating system.
     */
    private List<String> getDefaultTessdataPaths() {
        List<String> paths = new ArrayList<>();

        if (Platform.isWindows()) {
            paths.add(DEFAULT_WIN_TESSDATA_PATH);
            paths.add(DEFAULT_WIN_TESSDATA_PATH_X86);
        } else if (Platform.isMac()) {
            if (Platform.isARM()) {
                paths.add(DEFAULT_OSX_HOMEBREW_ARM_PATH);
            }
            paths.add(DEFAULT_OSX_TESSDATA_PATH);
        } else if (Platform.isLinux()) {
            paths.add(DEFAULT_LINUX_TESSDATA_PATH);
            paths.add(DEFAULT_LINUX_TESSDATA_PATH_ALT);
        }

        return paths;
    }
}
