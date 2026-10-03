package org.jabref.logic.ocr;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
class OcrResultTest {

    @Test
    void failureStoresImmutableSnapshotOfCommand() {
        ArrayList<String> command = new ArrayList<>(List.of("ocrmypdf", "--force-ocr"));
        OcrResult.Failure failure = new OcrResult.Failure(
                OcrFailureReason.NON_ZERO_EXIT,
                command,
                "output");

        command.add("later-change");

        assertEquals(List.of("ocrmypdf", "--force-ocr"), failure.command());
    }
}
