package org.jabref.logic.formatter.bibtexfields;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NormalizeISSNTest {

    private final NormalizeIssn formatISSN = new NormalizeIssn();

    @Test
    void returnValidIssnUnchanged() {
        assertEquals("0123-4567", formatISSN.format("0123-4567"));
    }

    @Test
    void addMissingDashToIssn() {
        assertEquals("0123-4567", formatISSN.format("01234567"));
    }

    @Test
    void leavesInvalidInputUnchanged() {
        assertEquals("Banana", formatISSN.format("Banana"));
    }

    @Test
    void emptyOrNullReturnsSame() {
        assertEquals("", formatISSN.format(""));
        assertThrows(NullPointerException.class, () -> formatISSN.format(null));
    }
}
