package org.jabref.gui.linkedfile;

import java.util.List;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.ocr.OcrFailureReason;
import org.jabref.logic.ocr.OcrResult;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class OcrLinkedFileActionTest {

    @Test
    void buildFailureMessageWithDiagnostics() {
        OcrResult.Failure failure = new OcrResult.Failure(
                OcrFailureReason.NON_ZERO_EXIT,
                List.of(
                        "ocrmypdf",
                        "--force-ocr",
                        "/Users/test/My Files/input.pdf"),
                "known output");

        String expected = "OCR process failed"
                + "\n\n"
                + Localization.lang(
                "Command\n%0",
                "\"ocrmypdf\""
                        + System.lineSeparator()
                        + "\"--force-ocr\""
                        + System.lineSeparator()
                        + "\"/Users/test/My Files/input.pdf\"")
                + "\n\n"
                + Localization.lang(
                "Output\n%0",
                "known output");

        assertEquals(
                expected,
                OcrLinkedFileAction.buildFailureMessage("OCR process failed", failure));
    }

    @Test
    void buildFailureMessageWithoutDiagnostics() {
        OcrResult.Failure failure = new OcrResult.Failure(OcrFailureReason.TIMEOUT);

        assertEquals(
                "OCR timed out",
                OcrLinkedFileAction.buildFailureMessage("OCR timed out", failure));
    }
}
