package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class NormalizeMonthFormatterTest {

    private static final NormalizeMonthFormatter formatter = new NormalizeMonthFormatter();

    @Test
    public void formatExample() {
        assertEquals("#dec#", formatter.format(formatter.getExampleInput()));
    }

    @Test
    public void plainAprilShouldBeApril() {
        assertEquals("#apr#", formatter.format("#apr#"));
    }
}
