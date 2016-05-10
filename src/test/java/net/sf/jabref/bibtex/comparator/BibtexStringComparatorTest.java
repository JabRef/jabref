package net.sf.jabref.bibtex.comparator;

import net.sf.jabref.model.entry.BibtexString;
import net.sf.jabref.model.entry.IdGenerator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BibtexStringComparatorTest {

    private final BibtexStringComparator bsc1 = new BibtexStringComparator(false);
    private final BibtexStringComparator bsc2 = new BibtexStringComparator(true);


    @Test
    public void test() {
        BibtexString bs1 = new BibtexString(IdGenerator.next(), "VLSI", "Very Large Scale Integration");
        BibtexString bs2 = new BibtexString(IdGenerator.next(), "DSP", "Digital Signal Processing");
        BibtexString bs3 = new BibtexString(IdGenerator.next(), "DSP", "Digital Signal Processing");

        // Same string
        assertEquals(0, bsc1.compare(bs1, bs1));
        // Same content
        assertEquals(0, bsc1.compare(bs2, bs3));
        // Alphabetical order
        assertTrue(bsc1.compare(bs1, bs2) > 0);
        assertTrue(bsc1.compare(bs2, bs1) < 0);

        // Same, but with the comparator checking for internal strings (none)
        assertEquals(0, bsc2.compare(bs1, bs1));
        assertEquals(0, bsc2.compare(bs2, bs3));
        assertTrue(bsc2.compare(bs1, bs2) > 0);
        assertTrue(bsc2.compare(bs2, bs1) < 0);

        // Create string with internal string
        BibtexString bs4 = new BibtexString(IdGenerator.next(), "DSPVLSI", "#VLSI# #DSP#");
        // bs4 before bs1 if not considering that bs4 contains bs1
        assertTrue(bsc1.compare(bs1, bs4) > 0);
        assertTrue(bsc1.compare(bs4, bs1) < 0);

        // bs4 after bs1 if considering that bs4 contains bs1
        assertTrue(bsc2.compare(bs1, bs4) < 0);
        assertTrue(bsc2.compare(bs4, bs1) > 0);

    }

}
