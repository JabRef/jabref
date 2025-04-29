package org.jabref.model.entry.identifier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ISSNTest {

    @Test
    void isCanBeCleaned() {
        assertTrue(new ISSN("00279633").isCanBeCleaned());
    }

    @Test
    void isCanBeCleanedIncorrectRubbish() {
        assertFalse(new ISSN("A brown fox").isCanBeCleaned());
    }

    @Test
    void isCanBeCleanedDashAlreadyThere() {
        assertFalse(new ISSN("0027-9633").isCanBeCleaned());
    }

    @Test
    void getCleanedISSN() {
        assertEquals("0027-9633", new ISSN("00279633").getCleanedISSN());
    }

    @Test
    void getCleanedISSNDashAlreadyThere() {
        assertEquals("0027-9633", new ISSN("0027-9633").getCleanedISSN());
    }

    @Test
    void getCleanedISSNDashRubbish() {
        assertEquals("A brown fox", new ISSN("A brown fox").getCleanedISSN());
    }

    @Test
    void isValidChecksumCorrect() {
        assertTrue(new ISSN("0027-9633").isValidChecksum());
        assertTrue(new ISSN("2434-561X").isValidChecksum());
        assertTrue(new ISSN("2434-561x").isValidChecksum());
    }

    @Test
    void isValidChecksumIncorrect() {
        assertFalse(new ISSN("0027-9634").isValidChecksum());
    }

    @Test
    void isValidFormatCorrect() {
        assertTrue(new ISSN("0027-963X").isValidFormat());
    }

    @Test
    void isValidFormatIncorrect() {
        assertFalse(new ISSN("00279634").isValidFormat());
    }
}
