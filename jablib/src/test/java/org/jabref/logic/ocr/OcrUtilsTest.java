package org.jabref.logic.ocr;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@NullMarked
class OcrUtilsTest {
    @Test
    void nonZeroExitPreservesCommandAndOutput() {
        String javaExecutable = ProcessHandle.current().info().command().orElseThrow();
        String classPath = System.getProperty("java.class.path");
        ArrayList<String> command = new ArrayList<>(List.of(javaExecutable, "-cp", classPath, FailingProcess.class.getName()));

        OcrResult result = OcrUtils.performOcr(command, "Test OCR");

        OcrResult.Failure failure = assertInstanceOf(OcrResult.Failure.class, result);
        assertEquals(OcrFailureReason.NON_ZERO_EXIT, failure.reason());
        assertEquals(command, failure.command());
        assertEquals("known test output" + System.lineSeparator(), failure.output());
    }

    static class FailingProcess {
        public static void main(String[] args) {
            System.out.println("known test output");
            System.exit(1);
        }
    }
}
