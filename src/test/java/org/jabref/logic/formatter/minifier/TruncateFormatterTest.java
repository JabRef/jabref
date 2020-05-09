package org.jabref.logic.formatter.minifier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class TruncateFormatterTest {
    private final String TITLE = "A Title";

    @Test
    void truncateModifierWorksWith0Length() {
        TruncateFormatter formatter = new TruncateFormatter(0);
        assertEquals("", formatter.format(TITLE));
    }

    @Test
    void truncateModifierRemovesTrailingWhitespace() {
        TruncateFormatter formatter = new TruncateFormatter(2);
        assertEquals("A", formatter.format(TITLE));
    }

    @Test
    void truncateModifierKeepsInternalWhitespace() {
        TruncateFormatter formatter = new TruncateFormatter(3);
        assertEquals("A T", formatter.format(TITLE));
    }

    @Test
    void truncateModifierWorksWith9999Length() {
        TruncateFormatter formatter = new TruncateFormatter(9999);
        assertEquals(TITLE, formatter.format(TITLE));
    }

    @Test
    void truncateModifierIgnoreNegativeIndex() {
        TruncateFormatter formatter = new TruncateFormatter(-1);
        assertEquals(TITLE, formatter.format(TITLE));
    }

    @Test
    void truncateModifierWorksWithEmptyString() {
        TruncateFormatter formatter = new TruncateFormatter(9999);
        assertEquals("", formatter.format(""));
    }

    @Test
    void truncateModifierThrowsExceptionNullString() {
        TruncateFormatter formatter = new TruncateFormatter(9999);
        assertThrows(NullPointerException.class, () -> formatter.format(null));
    }
}
