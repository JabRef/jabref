package net.sf.jabref.logic.formatter.bibtexfields;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.model.entry.BibtexEntry;

public class LatexFormatterTest {

    private BibtexEntry entry;


    @Before
    public void setUp() throws Exception {
        entry = new BibtexEntry();
    }

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
