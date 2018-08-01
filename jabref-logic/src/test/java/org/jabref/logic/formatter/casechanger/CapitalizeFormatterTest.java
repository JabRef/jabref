package org.jabref.logic.formatter.casechanger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class CapitalizeFormatterTest {

    private CapitalizeFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new CapitalizeFormatter();
    }

    @Test
    public void test() {
        assertEquals("Upper Each First", formatter.format("upper each First"));
        assertEquals("Upper Each First {NOT} {this}", formatter.format("upper each first {NOT} {this}"));
        assertEquals("Upper Each First {N}ot {t}his", formatter.format("upper each first {N}OT {t}his"));
    }

    @Test
    public void formatExample() {
        assertEquals("I Have {a} Dream", formatter.format(formatter.getExampleInput()));
    }

}
