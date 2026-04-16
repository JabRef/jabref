package org.jabref.logic.integrity;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ResourceLock("Localization.lang")
class BooktitleCheckerTest {

    private final BooktitleChecker checker = new BooktitleChecker();
    private final BooktitleContainsYearChecker yearChecker = new BooktitleContainsYearChecker();
    private final BooktitleContainsCountryChecker countryChecker = new BooktitleContainsCountryChecker();
    private final BooktitleContainsPagesChecker pagesChecker = new BooktitleContainsPagesChecker();

    // region "ends with conference on" checks

    @Test
    void booktitleAcceptsIfItDoesNotEndWithConferenceOn() {
        assertEquals(Optional.empty(), checker.checkValue("Fourth International Conference on Digital Information and Communication Technology and its Applications (DICTAP)"));
    }

    @Test
    void booktitleDoesNotAcceptIfItEndsWithConferenceOn() {
        assertEquals(Optional.of("booktitle ends with 'conference on'"),
                checker.checkValue("Digital Information and Communication Technology and its Applications (DICTAP), Fourth International Conference on"));
    }

    @Test
    void booktitleIsBlank() {
        assertEquals(Optional.empty(), checker.checkValue(" "));
    }

    // endregion

    // region Year detection

    @Test
    void booktitleFlagsYearInMiddle() {
        assertEquals(Optional.of("booktitle should not contain a year"),
                yearChecker.checkValue("European Conference on Circuit Theory and Design, {ECCTD} 2015, Trondheim, Norway"));
    }

    @Test
    void booktitleFlagsYearAtStart() {
        assertEquals(Optional.of("booktitle should not contain a year"),
                yearChecker.checkValue("2015 {IEEE} International Conference on Digital Signal Processing"));
    }

    @Test
    void booktitleAcceptsWhenNoYear() {
        assertEquals(Optional.empty(), yearChecker.checkValue("International Conference on Software Engineering"));
    }

    @Test
    void booktitleYearNotFlaggedInsideAlphanumericToken() {
        // "ICML2015" should NOT be flagged — the digits are part of a larger token
        assertEquals(Optional.empty(), yearChecker.checkValue("Proceedings ICML2015"));
    }

    @Test
    void booktitleYearCheckerIsBlank() {
        assertEquals(Optional.empty(), yearChecker.checkValue(" "));
    }

    // endregion

    // region Location (country) detection

    @Test
    void booktitleFlagsCountryName() {
        assertEquals(Optional.of("booktitle should not contain a location"),
                countryChecker.checkValue("Service-Oriented Computing, Fifth International Conference, Vienna, Austria, Proceedings"));
    }

    @Test
    void booktitleFlagsCountryNameSingapore() {
        assertEquals(Optional.of("booktitle should not contain a location"),
                countryChecker.checkValue("{IEEE} International Conference on Digital Signal Processing, Singapore, Proceedings"));
    }

    @Test
    void booktitleAcceptsWhenNoCountry() {
        assertEquals(Optional.empty(), countryChecker.checkValue("International Conference on Machine Learning Proceedings"));
    }

    @Test
    void booktitleCountryNotFlaggedInsideAlphanumericToken() {
        // "USA2015" should NOT be flagged — the abbreviation is part of a larger token
        assertEquals(Optional.empty(), countryChecker.checkValue("Proceedings USA2015"));
    }

    @Test
    void booktitleCountryCheckerIsBlank() {
        assertEquals(Optional.empty(), countryChecker.checkValue(" "));
    }

    // endregion

    // region Page-number detection

    @Test
    void booktitleFlagsPagesPattern() {
        assertEquals(Optional.of("booktitle should not contain page numbers"),
                pagesChecker.checkValue("Advances in Neural Information Processing Systems, pp. 1234-1242"));
    }

    @Test
    void booktitleFlagsPagesKeyword() {
        assertEquals(Optional.of("booktitle should not contain page numbers"),
                pagesChecker.checkValue("Advances in Neural Information Processing Systems, pages 1234-1242"));
    }

    @Test
    void booktitleAcceptsWhenNoPageNumbers() {
        assertEquals(Optional.empty(), pagesChecker.checkValue("Advances in Neural Information Processing Systems"));
    }

    @Test
    void booktitlePagesCheckerIsBlank() {
        assertEquals(Optional.empty(), pagesChecker.checkValue(" "));
    }

    // endregion

    // region Multiple issues in one booktitle

    @Test
    void booktitleWithYearAndCountryFlagsBoth() {
        // Year checker and country checker are separate — both fire independently
        assertEquals(Optional.of("booktitle should not contain a year"),
                yearChecker.checkValue("2015 IEEE Conference, Singapore"));
        assertEquals(Optional.of("booktitle should not contain a location"),
                countryChecker.checkValue("2015 IEEE Conference, Singapore"));
    }

    // endregion
}
