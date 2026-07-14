package org.jabref.logic.ocr;

import java.nio.file.Path;

import org.jabref.logic.util.io.FileUtil;

public final class OcrUtils {

    public static final String OCR_PDF_PREFIX = "_ocr.pdf";
    public static final int TIMEOUT_MINS = 10;
    public static final int CHECKING_TIMEOUT = 5;

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
