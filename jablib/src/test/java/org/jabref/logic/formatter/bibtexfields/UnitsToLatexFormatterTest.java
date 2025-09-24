package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

    @ParameterizedTest
    @CsvSource({"1~{A}, 1 A", "1\\mbox{-}{mA}, 1-mA"})
    void test(String expected, String text) {
        assertEquals(expected, formatter.format(text));
    }

    @Test
    void formatExample() {
        assertEquals("1~{Hz}", formatter.format(formatter.getExampleInput()));
    }
}
