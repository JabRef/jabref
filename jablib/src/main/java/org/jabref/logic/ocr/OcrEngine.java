package org.jabref.logic.ocr;

import java.nio.file.Path;

/// Interface for OCR engines.
///
/// Any engine in the future can implement this interface.
public interface OcrEngine {

    /// Performs OCR on the given input file and returns the result, whether the path of the OCRed with searchable text file or the error message.
    ///
    /// @param pdfPath the file to perform OCR on.
    /// @return the result of the OCR operation with the extracted text or an error message.
    OcrResult performOcrAndEmbedText(Path pdfPath);

    /// Checks if the OCR engine is available for use.
    ///
    /// @return true if the engine is available, false otherwise.
    boolean isAvailable();

    /// Gets the name of the OCR engine.
    ///
    /// @return the name of the OCR engine (e.g., "OCRmyPDF", "Tesseract").
    String getName();
}
