package org.jabref.logic.ocr;

import java.nio.file.Path;
import java.util.ArrayList;

import org.jabref.logic.util.strings.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Implementation of the [OcrEngine] interface using OCRmyPDF.
public class OcrMyPdfEngine implements OcrEngine {

    public static final Logger LOGGER = LoggerFactory.getLogger(OcrMyPdfEngine.class);
    private final OcrPreferences ocrPreferences;

    public OcrMyPdfEngine(OcrPreferences ocrPreferences) {
        this.ocrPreferences = ocrPreferences;
    }

    @Override
    public String getName() {
        return "OCRmyPDF";
    }

    /// OCRmyPDF writes the searchable PDF to a new file alongside the original file.
    ///
    /// Example: document.pdf -> document_ocr.pdf.
    ///
    /// @param pdfPath the file to perform OCR on.
    /// @return [OcrResult.Success] containing the path to the searchable PDF,
    /// or [OcrResult.Failure] with an error message if OCR failed.
    @Override
    public OcrResult performOcrAndEmbedText(Path pdfPath) {
        OcrResult availability = OcrUtils.isAvailable(ocrPreferences);
        if (availability.isFailure()) {
            OcrResult.Failure failure = (OcrResult.Failure) availability;
            return OcrResult.failure(OcrFailureReason.NOT_AVAILABLE, failure.commandLine(), failure.output());
        }
        Path outputPath = OcrUtils.makeOutputFilePath(pdfPath);
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
        OcrResult ocrResult = OcrUtils.performOcr(command, getName());
        if (ocrResult.isSuccess()) {
            return OcrResult.success(outputPath);
        } else {
            return ocrResult;
        }
    }
}
