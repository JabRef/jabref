package net.sf.jabref.logic.formatter.bibtexfields;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Tests in addition to the general tests from {@link net.sf.jabref.logic.formatter.FormatterTest}
 */
public class LatexCleanupFormatterTest {

    @Test
    public void test() {
        LatexCleanupFormatter lf = new LatexCleanupFormatter();

        assertEquals("$\\alpha\\beta$", lf.format("$\\alpha$$\\beta$"));
        assertEquals("{VLSI DSP}", lf.format("{VLSI} {DSP}"));
        assertEquals("\\textbf{VLSI} {DSP}", lf.format("\\textbf{VLSI} {DSP}"));

        assertEquals("A ${\\Delta\\Sigma}$ modulator for {FPGA DSP}",
                lf.format("A ${\\Delta}$${\\Sigma}$ modulator for {FPGA} {DSP}"));
    }

}
