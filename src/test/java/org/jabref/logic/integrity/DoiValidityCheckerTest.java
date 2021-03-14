package org.jabref.logic.integrity;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DoiValidityCheckerTest {

    private final DoiValidityChecker checker = new DoiValidityChecker();

    @Test
    void doiAcceptsValidInput() {
        assertEquals(Optional.empty(), checker.checkValue("10.1023/A:1022883727209"));
    }

    @Test
    void doiAcceptsEmptyInput() {
        assertEquals(Optional.empty(), checker.checkValue(""));
    }

    @Test
    void doiAcceptsValidInputWithNotOnlyNumbers() {
        assertEquals(Optional.empty(), checker.checkValue("10.17487/rfc1436"));
    }

    @Test
    void doiAcceptsValidInputNoMatterTheLengthOfTheDOIName() {
        assertEquals(Optional.empty(), checker.checkValue("10.1002/(SICI)1097-4571(199205)43:4<284::AID-ASI3>3.0.CO;2-0"));
    }

    @Test
    void doiDoesNotAcceptInvalidInput() {
        assertNotEquals(Optional.empty(), checker.checkValue("asdf"));
    }

    @Test
    void doiDoesNotAcceptInputWithTypoInFirstPart() {
        assertNotEquals(Optional.empty(), checker.checkValue("11.1000/182"));
        assertNotEquals(Optional.empty(), checker.checkValue("01.1000/182"));
        assertNotEquals(Optional.empty(), checker.checkValue("100.1000/182"));
        assertNotEquals(Optional.empty(), checker.checkValue("110.1000/182"));
        assertNotEquals(Optional.empty(), checker.checkValue("a10.1000/182"));
        assertNotEquals(Optional.empty(), checker.checkValue("10a.1000/182"));
    }

    @Test
    void doiDoesNotAcceptInputWithTypoInSecondPart() {
        assertNotEquals(Optional.empty(), checker.checkValue("10.a1000/182"));
        assertNotEquals(Optional.empty(), checker.checkValue("10.1000a/182"));
    }
}
