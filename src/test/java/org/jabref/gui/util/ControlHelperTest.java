package org.jabref.gui.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ControlHelperTest {

    private final String TEXT = "abcdef";
    private final int MAX_CHARACTERS = 5;
    private final int DEFAULT_MAX_CHARACTERS = -1;
    private final String ELLIPSIS_STRING = "***";
    private final ControlHelper.EllipsisPosition ELLIPSIS_POSITION = ControlHelper.EllipsisPosition.ENDING;

    @ParameterizedTest
    @NullAndEmptySource
    void truncateWithTextNullAndEmptyReturnsSource(String text) {
        String truncatedText = ControlHelper.truncateString(text, MAX_CHARACTERS, ELLIPSIS_STRING, ELLIPSIS_POSITION);
        assertEquals(text, truncatedText);
    }

    @Test
    void truncateWithDefaultMaxCharactersReturnsText() {
        String truncatedText = ControlHelper.truncateString(TEXT, DEFAULT_MAX_CHARACTERS, ELLIPSIS_STRING, ELLIPSIS_POSITION);
        assertEquals(TEXT, truncatedText);
    }

    @Test
    void truncateWithEllipsisPositionBeginningReturnsTruncatedText() {
        String truncatedText = ControlHelper.truncateString(TEXT, MAX_CHARACTERS, ELLIPSIS_STRING, ControlHelper.EllipsisPosition.BEGINNING);
        assertEquals("***ef", truncatedText);
    }

    @Test
    void truncateWithEllipsisPositionCenterReturnsTruncatedText() {
        String truncatedText = ControlHelper.truncateString(TEXT, MAX_CHARACTERS, ELLIPSIS_STRING, ControlHelper.EllipsisPosition.CENTER);
        assertEquals("a***f", truncatedText);
    }

    @Test
    void truncateWithDefaultMaxCharactersAndNullEllipsisAndPositionEndingReturnsTruncatedText() {
        String text = "a".repeat(75) + "b".repeat(25);
        String truncatedText = ControlHelper.truncateString(text, DEFAULT_MAX_CHARACTERS, null, ControlHelper.EllipsisPosition.ENDING);
        assertEquals("a".repeat(75), truncatedText);
    }

    @ParameterizedTest
    @NullSource
    void truncateWithNullEllipsisPositionThrowsNullPointerException(ControlHelper.EllipsisPosition ellipsisPosition) {
        assertThrows(
            NullPointerException.class,
            () -> ControlHelper.truncateString(TEXT, MAX_CHARACTERS, ELLIPSIS_STRING, ellipsisPosition)
        );
    }
}
