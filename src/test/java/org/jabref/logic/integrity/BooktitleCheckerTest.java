package org.jabref.logic.integrity;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BooktitleCheckerTest {

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
    void booktitleIsBlank() {
        assertEquals(Optional.empty(), checker.checkValue(" "));
    }

    @Test
    void booktitleShouldAcceptURLWithoutProtocol() {
        assertEquals(Optional.empty(), checker.checkValue("Applying {T}rip@dvice {R}ecommendation {T}echnology to www.visiteurope.com"));
    }

    @ParameterizedTest(name = "{index}. Booktitle: \"{0}\" should be invalid")
    @CsvSource({
            "Proceedings of the https://example.com/conference",
            "Find more at http://mywebsite.org/article",
            "Visit ftp://files.example.com/download",
    })
    void booktitleShouldRaiseWarning(String booktitle) {
        assertNotEquals(Optional.empty(), checker.checkValue(booktitle));
    }
}
