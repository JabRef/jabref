package org.jabref.logic.bibtex.comparator;

import org.jabref.model.entry.BibtexString;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BibtexStringComparatorTest {

    private final BibtexStringComparator bsc1 = new BibtexStringComparator(false);
    private final BibtexStringComparator bsc2 = new BibtexStringComparator(true);

    private final BibtexString bs1 = new BibtexString("VLSI", "Very Large Scale Integration");
    private final BibtexString bs2 = new BibtexString("DSP", "Digital Signal Processing");
    private final BibtexString bs3 = new BibtexString("DSP", "Digital Signal Processing");
    private final BibtexString bs4 = new BibtexString("DSPVLSI", "#VLSI# #DSP#");

    @Test
    void compareSameString() {
        assertEquals(0, bsc1.compare(bs1, bs1), "Error when comparing the same string");
    }

    @Test
    void compareSameContent() {
        assertEquals(0, bsc1.compare(bs2, bs3), "Different strings do not contain the same content");
    }

    @Test
    void compareStringsReverseAlphabeticallyOrdered() {
        assertTrue(bsc1.compare(bs1, bs2) > 0, "bs1 does not lexicographically succeed bs2");
    }

    @Test
    void compareStringsAlphabeticallyOrdered() {
        assertTrue(bsc1.compare(bs2, bs1) < 0, "bs2 does not lexicographically precede bs1");
    }

    @Test
    void compareSameStringWithInternalCheckingEnabled() {
        assertEquals(0, bsc2.compare(bs1, bs1), "Error when comparing the same string [internal checking enabled]");
    }

    @Test
    void compareSameContentWithInternalCheckingEnabled() {
        assertEquals(0, bsc2.compare(bs2, bs3), "Different strings do not contain the same content [internal checking enabled]");
    }

    @Test
    void compareStringsReverseAlphabeticallyOrderedWithInternalCheckingEnabled() {
        assertTrue(bsc2.compare(bs1, bs2) > 0, "bs1 does not succeed bs2 [internal checking enabled]");
    }

    @Test
    void compareStringsAlphabeticallyOrderedWithInternalCheckingEnabled() {
        assertTrue(bsc2.compare(bs2, bs1) < 0, "bs2 does not precede bs1 [internal checking enabled]");
    }

    @Test
    void compareRegularStringToInternalString() {
        assertTrue(bsc1.compare(bs1, bs4) > 0, "bs1 does not lexicographically succeed bs4");
    }

    @Test
    void compareInternalStringToRegularString() {
        assertTrue(bsc1.compare(bs4, bs1) < 0, "bs4 does not lexicographically precede bs1");
    }

    @Test
    void compareRegularStringToInternalStringWithInternalCheckingEnabled() {
        assertTrue(bsc2.compare(bs1, bs4) < 0, "bs4 does not contain bs1 [internal checking enabled]");
    }

    @Test
    void compareInternalStringToRegularStringWithInternalCheckingEnabled() {
        assertTrue(bsc2.compare(bs4, bs1) > 0, "bs4 contains bs1 [internal checking enabled]");
    }
}
