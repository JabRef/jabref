package org.jabref.logic.ocr;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OcrUtilsTest {

    @Test
    void performOcrWithMissingCommandReturnsIoErrorWithCommandLine() {
        ArrayList<String> command = new ArrayList<>(List.of("this-command-does-not-exist-jabref-ocr-test"));

        OcrResult result = OcrUtils.performOcr(command, "test-engine");

        OcrResult.Failure failure = assertInstanceOf(OcrResult.Failure.class, result);
        assertEquals(OcrFailureReason.IO_ERROR, failure.reason());
        assertEquals("this-command-does-not-exist-jabref-ocr-test", failure.commandLine());
        assertTrue(failure.output().isEmpty(), "No process ever started, so no output should have been captured");
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void performOcrWithFailingCommandCapturesCommandLineAndOutput() {
        ArrayList<String> command = new ArrayList<>(List.of(
                "sh", "-c", "echo jabref-ocr-test-output; exit 1"));

        OcrResult result = OcrUtils.performOcr(command, "test-engine");

        OcrResult.Failure failure = assertInstanceOf(OcrResult.Failure.class, result);
        assertEquals(OcrFailureReason.NON_ZERO_EXIT, failure.reason());
        assertEquals("sh -c echo jabref-ocr-test-output; exit 1", failure.commandLine());
        assertTrue(failure.output().contains("jabref-ocr-test-output"),
                "Expected captured output to contain the command's stdout, but was: " + failure.output());
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void performOcrWithSucceedingCommandReturnsSuccess() {
        ArrayList<String> command = new ArrayList<>(List.of("sh", "-c", "exit 0"));

        OcrResult result = OcrUtils.performOcr(command, "test-engine");

        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());
    }
}
