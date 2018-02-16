package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests in addition to the general tests from {@link org.jabref.logic.formatter.FormatterTest}
 */
public class LatexCleanupFormatterTest {

    private LatexCleanupFormatter formatter;

    @BeforeEach
    public void setUp() {
        formatter = new LatexCleanupFormatter();
    }

    @Test
    public void test() {
        assertEquals("$\\alpha\\beta$", formatter.format("$\\alpha$$\\beta$"));
        assertEquals("{VLSI DSP}", formatter.format("{VLSI} {DSP}"));
        assertEquals("\\textbf{VLSI} {DSP}", formatter.format("\\textbf{VLSI} {DSP}"));
        assertEquals("A ${\\Delta\\Sigma}$ modulator for {FPGA DSP}",
                formatter.format("A ${\\Delta}$${\\Sigma}$ modulator for {FPGA} {DSP}"));
    }

    @Test
    public void preservePercentSign() {
        assertEquals("\\%", formatter.format("%"));
    }

    @Test
    public void escapePercentSignOnlyOnce() {
        assertEquals("\\%", formatter.format("\\%"));
    }

    @Test
    public void escapePercentSignOnlnyOnceWithNumber() {
        assertEquals("50\\%", formatter.format("50\\%"));
    }

    @Test
    public void formatExample() {
        assertEquals("{VLSI DSP}", formatter.format(formatter.getExampleInput()));
    }

}
