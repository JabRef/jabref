package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
class UnitsToLatexFormatterTest {

    private UnitsToLatexFormatter formatter;

    @BeforeEach
    void setUp() {
        formatter = new UnitsToLatexFormatter();
    }

    @Test
    void test() {
        assertEquals("1~{A}", formatter.format("1 A"));
        assertEquals("1\\mbox{-}{mA}", formatter.format("1-mA"));
    }

    @Test
    void formatExample() {
        assertEquals("1~{Hz}", formatter.format(formatter.getExampleInput()));
    }
}
