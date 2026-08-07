package org.jabref.logic.bibtex.comparator;

import org.jabref.model.entry.BibtexString;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BibtexStringComparatorTest {

    private final BibtexStringComparator bsc1 = new BibtexStringComparator(false);
    private final BibtexStringComparator bsc2 = new BibtexStringComparator(true);

    private final BibtexString vlsiString = new BibtexString("VLSI", "Very Large Scale Integration");
    private final BibtexString dspString = new BibtexString("DSP", "Digital Signal Processing");
    private final BibtexString duplicateDspString = new BibtexString("DSP", "Digital Signal Processing");
    private final BibtexString dspVlsiCombined = new BibtexString("DSPVLSI", "#VLSI# #DSP#");

    @Test
    void compareSameString() {
        assertEquals(0, bsc1.compare(vlsiString, vlsiString), "Error when comparing the same string");
    }

    @Test
    void compareSameContent() {
        assertEquals(0, bsc1.compare(dspString, duplicateDspString), "Different strings do not contain the same content");
    }

    @Test
    void compareStringsReverseAlphabeticallyOrdered() {
        assertTrue(bsc1.compare(vlsiString, dspString) > 0, "vlsiString does not lexicographically succeed dspString");
    }

    @Test
    void compareStringsAlphabeticallyOrdered() {
        assertTrue(bsc1.compare(dspString, vlsiString) < 0, "dspString does not lexicographically precede vlsiString");
    }

    @Test
    void compareSameStringWithInternalCheckingEnabled() {
        assertEquals(0, bsc2.compare(vlsiString, vlsiString), "Error when comparing the same string [internal checking enabled]");
    }

    @Test
    void compareSameContentWithInternalCheckingEnabled() {
        assertEquals(0, bsc2.compare(dspString, duplicateDspString), "Different strings do not contain the same content [internal checking enabled]");
    }

    @Test
    void compareStringsReverseAlphabeticallyOrderedWithInternalCheckingEnabled() {
        assertTrue(bsc2.compare(vlsiString, dspString) > 0, "vlsiString does not succeed dspString [internal checking enabled]");
    }

    @Test
    void compareStringsAlphabeticallyOrderedWithInternalCheckingEnabled() {
        assertTrue(bsc2.compare(dspString, vlsiString) < 0, "dspString does not precede vlsiString [internal checking enabled]");
    }

    @Test
    void compareRegularStringToInternalString() {
        assertTrue(bsc1.compare(vlsiString, dspVlsiCombined) > 0, "vlsiString does not lexicographically succeed dspVlsiCombined");
    }

    @Test
    void compareInternalStringToRegularString() {
        assertTrue(bsc1.compare(dspVlsiCombined, vlsiString) < 0, "dspVlsiCombined does not lexicographically precede vlsiString");
    }

    @Test
    void compareRegularStringToInternalStringWithInternalCheckingEnabled() {
        assertTrue(bsc2.compare(vlsiString, dspVlsiCombined) < 0, "dspVlsiCombined does not contain vlsiString [internal checking enabled]");
    }

    @Test
    void compareInternalStringToRegularStringWithInternalCheckingEnabled() {
        assertTrue(bsc2.compare(dspVlsiCombined, vlsiString) > 0, "dspVlsiCombined contains vlsiString [internal checking enabled]");
    }
}
