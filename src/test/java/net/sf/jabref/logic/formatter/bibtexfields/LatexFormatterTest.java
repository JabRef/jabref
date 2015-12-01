package net.sf.jabref.logic.formatter.bibtexfields;

import static org.junit.Assert.*;

import org.junit.Test;

public class LatexFormatterTest {

    @Test
    public void test() {
        LatexFormatter lf = new LatexFormatter();

        assertEquals("$\\alpha\\beta$", lf.format("$\\alpha$$\\beta$"));
        assertEquals("{VLSI DSP}", lf.format("{VLSI} {DSP}"));
        assertEquals("\\textbf{VLSI} {DSP}", lf.format("\\textbf{VLSI} {DSP}"));

        assertEquals("A ${\\Delta\\Sigma}$ modulator for {FPGA DSP}",
                lf.format("A ${\\Delta}$${\\Sigma}$ modulator for {FPGA} {DSP}"));
    }

}
