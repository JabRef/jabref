package org.jabref.logic.integrity;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@ResourceLock("Localization.lang")
class BooktitleCheckerTest {

    private final BooktitleChecker checker = new BooktitleChecker();

    // ------------------------------------------------------------------
    // Existing "ends with conference on" checks
    // ------------------------------------------------------------------

    @Test
    void booktitleAcceptsIfItDoesNotEndWithConferenceOn() {
        assertEquals(Optional.empty(), checker.checkValue("Fourth International Conference on Digital Information and Communication Technology and its Applications (DICTAP)"));
    }

    @Test
    void booktitleDoesNotAcceptIfItEndsWithConferenceOn() {
        assertNotEquals(Optional.empty(), checker.checkValue("Digital Information and Communication Technology and its Applications (DICTAP), Fourth International Conference on"));
    }

    @Test
    void booktitleIsBlank() {
        assertEquals(Optional.empty(), checker.checkValue(" "));
    }

    // ------------------------------------------------------------------
    // Year detection
    // ------------------------------------------------------------------

    @Test
    void booktitleFlagsYearInMiddle() {
        // Example from the issue: year embedded inside a booktitle
        assertNotEquals(Optional.empty(), checker.checkValue("European Conference on Circuit Theory and Design, {ECCTD} 2015, Trondheim, Norway"));
    }

    @Test
    void booktitleFlagsYearAtStart() {
        assertNotEquals(Optional.empty(), checker.checkValue("2015 {IEEE} International Conference on Digital Signal Processing"));
    }

    @Test
    void booktitleAcceptsWhenNoYear() {
        assertEquals(Optional.empty(), checker.checkValue("International Conference on Software Engineering"));
    }

    // ------------------------------------------------------------------
    // Location (country) detection
    // ------------------------------------------------------------------

    @Test
    void booktitleFlagsCountryName() {
        // "Norway" is a country and should be flagged
        assertNotEquals(Optional.empty(), checker.checkValue("Service-Oriented Computing, Fifth International Conference, Vienna, Austria, Proceedings"));
    }

    @Test
    void booktitleFlagsCountryNameSingapore() {
        assertNotEquals(Optional.empty(), checker.checkValue("{IEEE} International Conference on Digital Signal Processing, Singapore, Proceedings"));
    }

    @Test
    void booktitleAcceptsWhenNoCountry() {
        assertEquals(Optional.empty(), checker.checkValue("International Conference on Machine Learning Proceedings"));
    }

    // ------------------------------------------------------------------
    // Page-number detection
    // ------------------------------------------------------------------

    @Test
    void booktitleFlagsPagesPattern() {
        assertNotEquals(Optional.empty(), checker.checkValue("Advances in Neural Information Processing Systems, pp. 1234-1242"));
    }

    @Test
    void booktitleFlagsPagesKeyword() {
        assertNotEquals(Optional.empty(), checker.checkValue("Advances in Neural Information Processing Systems, pages 1234-1242"));
    }

    @Test
    void booktitleAcceptsWhenNoPageNumbers() {
        assertEquals(Optional.empty(), checker.checkValue("Advances in Neural Information Processing Systems"));
    }
}
