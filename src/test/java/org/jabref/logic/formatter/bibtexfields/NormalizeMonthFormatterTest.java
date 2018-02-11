package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class NormalizeMonthFormatterTest {

    private NormalizeMonthFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new NormalizeMonthFormatter();
    }

    @Test
    public void formatExample() {
        assertEquals("#dec#", formatter.format(formatter.getExampleInput()));
    }

}
