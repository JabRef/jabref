package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class UnitsToLatexFormatterTest {

    private UnitsToLatexFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new UnitsToLatexFormatter();
    }

    @Test
    public void test() {
        assertEquals("1~{A}", formatter.format("1 A"));
        assertEquals("1\\mbox{-}{mA}", formatter.format("1-mA"));
    }

    @Test
    public void formatExample() {
        assertEquals("1~{Hz}", formatter.format(formatter.getExampleInput()));
    }
}
