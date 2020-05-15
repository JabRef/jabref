package org.jabref.logic.integrity;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class BracketCheckerTest {

    private final BracketChecker checker = new BracketChecker();

    @Test
    void fieldAcceptsNoBrackets() {
        assertEquals(Optional.empty(), checker.checkValue("x"));
    }

    @Test
    void fieldAcceptsEvenNumberOfBrackets() {
        assertEquals(Optional.empty(), checker.checkValue("{x}"));
    }

    @Test
    void fieldAcceptsExpectedBracket() {
        assertEquals(Optional.empty(), checker.checkValue("{x}x{}x{{}}"));
    }

    @Test
    void fieldDoesNotAcceptOddNumberOfBrackets() {
        assertNotEquals(Optional.empty(), checker.checkValue("{x}x{}}x{{}}"));
    }

    @Test
    void fieldDoesNotAcceptUnexpectedClosingBracket() {
        assertNotEquals(Optional.empty(), checker.checkValue("}"));
    }

    @Test
    void fieldDoesNotAcceptUnexpectedOpeningBracket() {
        assertNotEquals(Optional.empty(), checker.checkValue("{"));
    }

}
