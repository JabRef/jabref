package net.sf.jabref.logic.cleanup;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import net.sf.jabref.model.entry.BibtexEntry;

public class LatexCleanupTest {

    private BibtexEntry entry;

    @Before
    public void setUp() throws Exception {
        entry = new BibtexEntry();
    }

    @Test
    public void test() {
        LatexCleanup cu = new LatexCleanup();

        cu.cleanup(); // Should work without entry

        assertEquals("$\\alpha\\beta$", cu.format("$\\alpha$$\\beta$"));
        assertEquals("{VLSI DSP}", cu.format("{VLSI} {DSP}"));
        assertEquals("\\textbf{VLSI} {DSP}", cu.format("\\textbf{VLSI} {DSP}"));

        entry.setField("title", "A ${\\Delta}$${\\Sigma}$ modulator for {FPGA} {DSP}");
        LatexCleanup cu2 = new LatexCleanup(entry);
        cu2.cleanup();
        assertEquals("A ${\\Delta\\Sigma}$ modulator for {FPGA DSP}", entry.getField("title"));
    }

}
