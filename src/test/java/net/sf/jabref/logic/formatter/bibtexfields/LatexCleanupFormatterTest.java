package net.sf.jabref.logic.formatter.bibtexfields;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests in addition to the general tests from {@link net.sf.jabref.logic.formatter.FormatterTest}
 */
public class LatexCleanupFormatterTest {

    private LatexCleanupFormatter formatter;

    @Before
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
    public void formatExample() {
        assertEquals("{VLSI DSP}", formatter.format(formatter.getExampleInput()));
    }


}
