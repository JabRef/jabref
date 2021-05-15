package org.jabref.logic.bibtex.comparator;

import org.jabref.model.entry.BibtexString;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BibtexStringComparatorTest {

    private final BibtexStringComparator bsc1 = new BibtexStringComparator(false);
    private final BibtexStringComparator bsc2 = new BibtexStringComparator(true);

    @Test
    public void test() {
        BibtexString bs1 = new BibtexString("VLSI", "Very Large Scale Integration");
        BibtexString bs2 = new BibtexString("DSP", "Digital Signal Processing");
        BibtexString bs3 = new BibtexString("DSP", "Digital Signal Processing");

        // Same string
        assertEquals(0, bsc1.compare(bs1, bs1), "Error when comparing the same string");
        // Same content
        assertEquals(0, bsc1.compare(bs2, bs3), "Different strings do not contain the same content");
        // Alphabetical order
        assertTrue(bsc1.compare(bs1, bs2) > 0, "bs1 does not lexicographically succeed bs2");
        assertTrue(bsc1.compare(bs2, bs1) < 0, "bs2 does not lexicographically precede bs1");

        // Same, but with the comparator checking for internal strings (none)
        assertEquals(0, bsc2.compare(bs1, bs1), "Error when comparing the same string [internal checking enabled]");
        assertEquals(0, bsc2.compare(bs2, bs3), "Different strings do not contain the same content [internal checking enabled]");
        assertTrue(bsc2.compare(bs1, bs2) > 0, "bs1 does not succeed bs2 [internal checking enabled]");
        assertTrue(bsc2.compare(bs2, bs1) < 0, "bs2 does not precede bs1 [internal checking enabled]");

        // Create string with internal string
        BibtexString bs4 = new BibtexString("DSPVLSI", "#VLSI# #DSP#");
        // bs4 before bs1 if not considering that bs4 contains bs1
        assertTrue(bsc1.compare(bs1, bs4) > 0, "bs1 does not lexicographically succeed bs4");
        assertTrue(bsc1.compare(bs4, bs1) < 0, "bs4 does not lexicographically precede bs1");

        // bs4 after bs1 if considering that bs4 contains bs1
        assertTrue(bsc2.compare(bs1, bs4) < 0, "bs4 does not contain bs1 [internal checking enabled]");
        assertTrue(bsc2.compare(bs4, bs1) > 0, "bs4 contains bs1 [internal checking enabled]");
    }
}
