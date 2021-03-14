package org.jabref.logic.integrity;

import java.util.Optional;

import org.jabref.logic.l10n.Localization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ISSNCheckerTest {

    private final ISSNChecker checker = new ISSNChecker();

    @Test
    void issnAcceptsValidInput() {
        assertEquals(Optional.empty(), checker.checkValue("0020-7217"));
    }

    @Test
    void issnAcceptsNumbersAndCharacters() {
        assertEquals(Optional.empty(), checker.checkValue("2434-561x"));
    }

    @Test
    void issnDoesNotAcceptRandomInput() {
        assertNotEquals(Optional.empty(), checker.checkValue("Some other stuff"));
    }

    @Test
    void issnDoesNotAcceptInvalidInput() {
        assertNotEquals(Optional.empty(), checker.checkValue("0020-7218"));
    }

    @Test
    void lessThanEightDigitCode() {
        assertEquals(Optional.of(Localization.lang("incorrect format")), checker.checkValue("020-721"));
    }

    @Test
    void moreThanEightDigitCode() {
        assertEquals(Optional.of(Localization.lang("incorrect format")), checker.checkValue("0020-72109"));
    }

    @Test
    void issnDividedByWrongCharacter() {
        assertEquals(Optional.of(Localization.lang("incorrect format")), checker.checkValue("0020~72109"));
    }

    @Test
    void emptyIssnValue() {
        assertEquals(Optional.empty(), checker.checkValue(""));
    }

}
