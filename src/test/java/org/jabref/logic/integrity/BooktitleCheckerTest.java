package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.logic.l10n.Localization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class BooktitleCheckerTest {

    private final BooktitleChecker checker = new BooktitleChecker();

    @Test
    void booktitleAcceptsIfItDoesNotEndWithConferenceOn() {
        assertEquals(Optional.empty(), checker.checkValue("2014 Fourth International Conference on Digital Information and Communication Technology and it's Applications (DICTAP)"));
    }

    @Test
    void booktitleDoesNotAcceptsIfItEndsWithConferenceOn() {
        assertNotEquals(Optional.empty(), checker.checkValue("Digital Information and Communication Technology and it's Applications (DICTAP), 2014 Fourth International Conference on"));
    }

    @Test
    void booktitleContainsDate() {
        assertEquals(Optional.of(Localization.lang("Date is contained in the booktitle")), checker.checkValue("Service-Oriented Computing - {ICSOC} 2007, Fifth International Conference, Vienna, Austria, September 17-20, 2007, Proceedings"));
    }

    @Test
    void booktitleContainsYear() {
        assertEquals(Optional.of(Localization.lang("Year is contained in the booktitle")), checker.checkValue("Service-Oriented Computing - {ICSOC} 2007, Fifth International Conference, Vienna, Austria, Proceedings"));
    }

    @Test
    void booktitleContainsAuthor() {
        assertEquals(Optional.of(Localization.lang("Author is contained in the booktitle")), checker.checkValue("Service-Oriented Computing - {ICSOC}, Fifth International Conference, Vienna, Austria, Proceedings"));
    }

    @Test
    void booktitleContainsPageNumbers() {
        assertEquals(Optional.of(Localization.lang("Page numbers are contained in the booktitle")), checker.checkValue("Service-Oriented Computing, Fifth International Conference, Vienna, Austria, Proceedings, Page 3-5"));
    }

    @Test
    void booktitleIsBlank() {
        assertEquals(Optional.empty(), checker.checkValue(" "));
    }
}
